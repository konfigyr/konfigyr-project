package com.konfigyr.vault.state;

import com.konfigyr.namespace.Service;
import com.konfigyr.vault.Profile;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.errors.CorruptObjectException;
import org.eclipse.jgit.errors.IndexReadException;
import org.eclipse.jgit.errors.IndexWriteException;
import org.eclipse.jgit.errors.RepositoryNotFoundException;
import org.eclipse.jgit.lib.*;
import org.eclipse.jgit.merge.MergeStrategy;
import org.eclipse.jgit.merge.ResolveMerger;
import org.eclipse.jgit.merge.ThreeWayMerger;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevSort;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.PathFilter;
import org.eclipse.jgit.util.FS;
import org.jspecify.annotations.NullMarked;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.util.function.ThrowingSupplier;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static com.konfigyr.vault.state.RepositoryStateException.ErrorCode.*;
import static com.konfigyr.vault.state.RepositoryStateException.ErrorCode.INVALID_STATE;
import static com.konfigyr.vault.state.RepositoryStateException.ErrorCode.UNAVAILABLE;

/**
 * Implementation of a {@link StateRepository} that uses a high-performance, low-level service for managing
 * Git repositories via the <b>Eclipse JGit</b> library.
 * <p>
 * Unlike standard Git implementations that rely on a working directory and Command Line Interface (CLI)
 * wrappers, this component interacts directly with the Git Object Database (ODB).
 * <h3>Core Architecture Principles</h3>
 * <ul>
 *     <li>
 *         <b>Bare Repository Focus:</b> All operations are designed for <i>bare</i> repositories, repos
 *         without a physical working tree. This way we can eliminate expensive Disk I/O operations associated
 *         with checking out files.
 *     </li>
 *     <li>
 *         <b>In-Core Processing:</b> Operations such as merging, squashing, and tree manipulation are performed
 *         in memory using the {@link ResolveMerger} and {@link ObjectInserter} APIs.
 *     </li>
 *     <li>
 *         <b>Reference Direct Access:</b> We bypass Porcelain commands in favor of {@link RefUpdate} for atomic
 *         pointer manipulation (branch creation, deletion, and HEAD management).
 *     </li>
 * </ul>
 * <h3>Best Practices for Developers</h3>
 * When updating the logic in this class, you <b>MUST</b> adhere to the following:
 * <ul>
 *     <li>
 *         <b>Avoid Porcelain:</b> Do not use the {@link org.eclipse.jgit.api.Git} wrapper class
 *         (e.g., {@code git.add()}, {@code git.commit()}). These classes often expect a Working Directory
 *         and involve unnecessary overhead.
 *     </li>
 *     <li>
 *         <b>Resource Management:</b> Always use try-with-resources for {@link Repository}, {@link RevWalk},
 *         {@link TreeWalk}, and {@link ObjectInserter}. Failure to close these will lead to file-lock leaks
 *         and memory exhaustion.
 *     </li>
 *     <li>
 *         <b>Atomic Updates:</b> When modifying multiple refs or objects, ensure you {@code flush()} the
 *         {@link ObjectInserter} before updating the {@link RefUpdate}. A reference should never point to
 *         an object that hasn't been persisted yet.
 *     </li>
 *     <li>
 *         <b>Thread Safety:</b> While JGit is generally thread-safe for reading, writing to a single
 *         repository instance should be synchronized or managed via JGit's internal locking mechanisms
 *         to avoid {@code LockFailure}.
 *     </li>
 *     <li>
 *         <b>The <i>Unborn</i> State:</b> Always check if {@code repository.resolve(HEAD)} is {@literal null}.
 *         New repositories require specific logic to handle the initial commit before standard graph traversal
 *         can begin. This is why when initializing, we create an empty commit to avoid this state.
 *     </li>
 * </ul>
 * <h3>Performance Warnings</h3>
 * <ul>
 *     <li>
 *         Do not load large Blobs into memory as {@code byte[]} or {@code String}. Always use the
 *         {@link ObjectLoader} to load or process data.
 *     </li>
 *     <li>
 *         Keep the {@link RevWalk} shallow whenever possible. Avoid parsing the full commit body if you only
 *         need the {@link ObjectId}.
 *     </li>
 * </ul>
 *
 * @author Vladimir Spasic
 * @since 1.0.0
 * @see org.eclipse.jgit.lib.Repository
 * @see org.eclipse.jgit.lib.ObjectInserter
 * @see org.eclipse.jgit.revwalk.RevWalk
 */
@Slf4j
@NullMarked
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class GitStateRepository implements StateRepository {

	/**
	 * The name of the file withing the Git repository that stores the configuration state.
	 */
	static final String CONFIGURATION_STATE_FILE_NAME = "application.properties";

	/**
	 * The path filter used to identify configuration state files when walking the Git repository tree.
	 */
	static final PathFilter CONFIGURATION_STATE_PATH_FILTER = PathFilter.create(CONFIGURATION_STATE_FILE_NAME);

	private final Service service;
	private final Repository repository;

	private volatile boolean closed = false;

	/**
	 * Creates a new instance of the {@link GitStateRepository} by loading the underlying Git {@link Repository}
	 * from the given root Git directory location.
	 *
	 * @param service the repository owner, can't be {@literal null}
	 * @param directory the root Git directory location, can't be {@literal null}
	 * @return the Git backed state repository, never {@literal null}
	 * @throws RepositoryStateException when there was an issue while loading the Git repository
	 */
	public static GitStateRepository load(Service service, Path directory) {
		final Path location = createRepositoryLocation(directory, service);

		log.debug("Loading repository for Service({}) with location: {}", service.id(), location);

		try {
			final RepositoryCache.FileKey key = RepositoryCache.FileKey.lenient(location.toFile(), FS.DETECTED);

			final Repository repository = new RepositoryBuilder()
					.setFS(FS.DETECTED)
					.setGitDir(key.getFile())
					.setMustExist(true)
					.build();

			return new GitStateRepository(service, repository);
		} catch (RepositoryNotFoundException ex) {
			throw new RepositoryStateException(UNKNOWN_REPOSITORY, "Could not find repository for Service(%s, %s)"
					.formatted(service.id(), service.slug()), ex);
		} catch (CorruptObjectException ex) {
			throw new RepositoryStateException(INVALID_STATE, "The Git repository for Service(%s, %s) is corrupted"
					.formatted(service.id(), service.slug()), ex);
		} catch (IOException ex) {
			throw new RepositoryStateException(UNAVAILABLE,
					"I/O failure while accessing or executing Git repository operation for Service(%s, %s)"
							.formatted(service.id(), service.slug()), ex);
		}
	}

	/**
	 * Initializes the source control repository for the given {@link Service} under the supplied
	 * root Git directory location
	 * <p>
	 * This method is responsible for creating the Bare Git {@link Repository} and any required default
	 * state. It is expected to be called exactly once during the service lifecycle.
	 *
	 * @param service the repository owner, can't be {@literal null}
	 * @param directory the root Git directory location, can't be {@literal null}
	 * @return initialized Git backed state repository, never {@literal null}
	 * @throws RepositoryStateException when there was an issue while initializing the Git repository
	 */
	public static GitStateRepository initialize(Service service, Path directory) {
		final Path location = createRepositoryLocation(directory, service);

		if (Files.exists(location)) {
			throw new RepositoryStateException(REPOSITORY_ALREADY_EXISTS, "Repository already exists for Service(%s, %s)"
					.formatted(service.id(), service.slug()));
		}

		log.debug("Initializing repository for Service({}) with location: {}", service.id(), location);

		// Create a new bare Git repository for the service as we intend to interact with Git the database
		// directly, not git work tree. To properly set up the repository and the HEAD state, an empty commit
		// would be created, this would then be used as a starting point when creating profile refs
		try {
			final Repository repository = FileRepositoryBuilder.create(location.toFile());
			repository.create(true);

			// Let's try to create a commit with an empty tree (no files) and update the main ref
			try (ObjectInserter inserter = repository.newObjectInserter()) {
				final CommitBuilder commit = new CommitBuilder();
				commit.setTreeId(inserter.insert(new TreeFormatter()));
				commit.setAuthor(new PersonIdent(repository));
				commit.setCommitter(new PersonIdent(repository));
				commit.setMessage("Repository initialized for Service(%s, %s)".formatted(service.id(), service.slug()));

				final ObjectId commitId = inserter.insert(commit);
				inserter.flush();

				final RefUpdate update = repository.updateRef(Constants.HEAD);
				update.setNewObjectId(commitId);
				final RefUpdate.Result result = update.update();

				// as this is a fresh repository, the ref update result must be new; anything else
				// should be considered as a repository initialization failure.
				if (result != RefUpdate.Result.NEW) {
					throw new RepositoryStateException(INITIALIZATION_FAILED,
							"Failed to create initial commit for Service(%s, %s) with result: %s".formatted(
									service.id(), service.slug(), result
							)
					);
				}
			}

			return new GitStateRepository(service, repository);
		} catch (RepositoryStateException ex) {
			throw ex;
		} catch (Exception ex) {
			throw new RepositoryStateException(INITIALIZATION_FAILED,
					"Unexpected error occurred while initializing Git Repository for Service(%s, %s)"
							.formatted(service.id(), service.slug()), ex);
		}
	}

	@Override
	public Service owner() {
		return service;
	}

	@Override
	public String create(Profile profile) {
		// In Git, a branch is just a small text file in .git/refs/heads/ containing a 40-character commit hash.
		// We do not need to create the branch using the CLI-style commands, let's create that reference manually,
		// using the Low-Level approach via RefUpdate. This approach is the most performant way because it skips the
		// overhead of the command validation layers and talks directly to the Git reference database.
		return executeRepositoryOperation(() -> {
			final String referenceName = formatProfileRefName(profile);

			// let's check first if the profile branch already exists, if it does, throw the exception
			if (repository.findRef(referenceName) != null) {
				throw new RepositoryStateException(PROFILE_ALREADY_EXISTS, "Profile '%s' already exists for Service(%s, %s)"
						.formatted(profile.slug(), service.id(), service.slug()));
			}

			log.debug("Attempting to create profile branch '{}' for Service({})", referenceName, service.id());

			// We need to create the RefUpdate with the full reference name (e.g., refs/heads/profile/new-profile)
			final RefUpdate update = repository.updateRef(referenceName);

			// Now we need to set the destination hash, the commit the branch will point to. By default,
			// it's the current HEAD. In the future we should allow creating profiles from other profile branches
			update.setNewObjectId(repository.resolve(Constants.HEAD));
			final RefUpdate.Result result = update.update();

			return switch (result) {
				case NEW, FAST_FORWARD, NO_CHANGE, RENAMED -> {
					log.info("Successfully created a new profile branch with name '{}' for Service({})",
							referenceName, service.id());

					yield update.getNewObjectId().getName();
				}
				default -> throw createSourceControlExceptionForRefUpdateResult(update, result,
						"Failed to create Git ref for profile '%s' owned by Service(%s, %s)".formatted(
								profile.slug(), service.id(), service.slug())
				);
			};
		});
	}

	@Override
	public RepositoryState get(Profile profile) {
		return executeRepositoryOperation(() -> {
			final ObjectId branchId = repository.resolve(formatProfileRefName(profile));

			if (branchId == null) {
				throw new RepositoryStateException(UNKNOWN_PROFILE,
						"Failed to retrieve state from profile '%s' as it does not exist for Service(%s, %s)"
								.formatted(profile.slug(), service.id(), service.slug()));
			}

			return loadState(branchId);
		});
	}

	@Override
	public RepositoryState get(Profile profile, String changeset) {
		return executeRepositoryOperation(() -> {
			final ObjectId branchId = repository.resolve(formatChangesetRefName(profile, changeset));

			if (branchId == null) {
				throw new RepositoryStateException(UNKNOWN_CHANGESET,
						"Failed to retrieve state from profile '%s' and changeset '%s' as it does not exist for Service(%s, %s)"
								.formatted(profile.slug(), changeset, service.id(), service.slug()));
			}

			return loadState(branchId);
		});
	}

	@Override
	public MergeOutcome update(Profile profile, Changeset changeset) {
		return executeRepositoryOperation(() -> {
			log.debug("Attempting to update configuration state for profile '{}'", profile);

			final ObjectId profileObjectId = repository.resolve(formatProfileRefName(profile));
			if (profileObjectId == null) {
				throw new RepositoryStateException(UNKNOWN_PROFILE,
						"Could not create changeset branch for an unknown profile with name '%s' for Service(%s, %s)"
								.formatted(profile.slug(), service.id(), service.slug()));
			}

			final String changesetBranchName = createChangesetBranchForProfile(profile, profileObjectId);

			log.debug("Attempting to update changeset branch '{}' for Service({}) with: {}",
					changesetBranchName, service.id(), changeset);

			// Use the ObjectInserter to update the file contents. This is not exactly 'editing' of an
			// existing file, we are creating a brand-new Git Blob, and then building a new Tree that points
			// to that new Blob instead of the old one. This is the most performant way to modify a repository
			// without touching the physical disk.
			try (ObjectInserter inserter = repository.newObjectInserter(); RevWalk walker = new RevWalk(repository)) {
				// First, we need to create a Blob for the new content
				final InputStream contentsStream = changeset.getInputStream();
				final ObjectId newBlobId = inserter.insert(Constants.OBJ_BLOB,
						contentsStream.available(), contentsStream);

				// then resolve the current Tree from the changeset branch head
				final ObjectId headId = repository.resolve(changesetBranchName);
				final RevCommit headCommit = walker.parseCommit(headId);
				final RevTree headTree = headCommit.getTree();

				// Use TreeFormatter to build a new Tree and copy any existing entries except for
				// the one we are changing the configuration state file should be present in the current tree
				final TreeFormatter formatter = new TreeFormatter();
				try (TreeWalk tree = new TreeWalk(repository)) {
					tree.addTree(headTree);
					// no need for a recursive tree walk, the file we are changing is in the
					// root repository directory
					tree.setRecursive(false);

					boolean found = false;
					while (tree.next()) {
						// Replace the old Blob ID with our new one, or copy existing entries as-is
						if (CONFIGURATION_STATE_PATH_FILTER.matchFilter(tree) == 0) {
							formatter.append(tree.getNameString(), tree.getFileMode(0), newBlobId);
							found = true;
						} else {
							formatter.append(tree.getNameString(), tree.getFileMode(0), tree.getObjectId(0));
						}
					}

					// If the file didn't exist, append it as a new entry
					if (!found) {
						formatter.append(CONFIGURATION_STATE_FILE_NAME, FileMode.REGULAR_FILE, newBlobId);
					}
				}

				// Create the Git person information from the changeset author
				final PersonIdent author = GitConverters.convertToPersonIdent(changeset.author());
				// Crete the Git commit message
				final String message = GitConverters.convertToCommitMessage(changeset.changes());

				// Now that we have a new Tree, we need to write it and commit it
				final CommitBuilder commit = new CommitBuilder();
				commit.setTreeId(inserter.insert(formatter));
				commit.setParentId(headId);
				commit.setMessage(message);
				commit.setAuthor(author);
				commit.setCommitter(author);

				// Generate the new commit identifier and update the current HEAD reference
				// for the given changeset branch
				final ObjectId newCommitId = inserter.insert(commit);
				inserter.flush();

				final RefUpdate update = repository.updateRef(changesetBranchName);
				update.setNewObjectId(newCommitId);

				final RefUpdate.Result result = update.update();

				switch (result) {
					case FAST_FORWARD, NO_CHANGE -> log.info(
							"Successfully updated changeset '{}' with commit '{}' for Service({}) with result: {}",
							changesetBranchName, newCommitId.name(), service.id(), result
					);
					default -> throw createSourceControlExceptionForRefUpdateResult(update, result,
							"Failed to update the state of Git ref for changeset '%s' owned by Service(%s, %s)".formatted(
									changesetBranchName, service.id(), service.slug())
					);
				}

				return MergeOutcome.applied(changesetBranchName, GitConverters.formatPerson(author), newCommitId.name());
			}
		});
	}

	@Override
	public MergeOutcome merge(Profile profile, String changeset) {
		return executeRepositoryOperation(() -> {
			log.debug("Attempting to apply changeset '{}' to profile: {}", changeset, profile);

			final ObjectId target = repository.resolve(formatProfileRefName(profile));

			if (target == null) {
				throw new RepositoryStateException(UNKNOWN_PROFILE,
						"Could not apply changeset '%s' to an unknown profile with name '%s' for Service(%s, %s)"
								.formatted(changeset, profile.slug(), service.id(), service.slug()));
			}

			final ObjectId source = repository.resolve(formatChangesetRefName(profile, changeset));

			if (source == null) {
				throw new RepositoryStateException(UNKNOWN_CHANGESET,
						"Failed to apply changes to profile '%s' as changeset '%s' does not exist for Service(%s, %s)"
								.formatted(profile.slug(), changeset, service.id(), service.slug()));
			}

			try (RevWalk walker = new RevWalk(repository); ObjectInserter inserter = repository.newObjectInserter()) {
				final RevCommit targetCommit = walker.parseCommit(target);
				final RevCommit sourceCommit = walker.parseCommit(source);

				// Use a RecursiveMerger that performs the merge logic in memory without a working directory
				// and resolve the latest commits from both the source and target branches
				final ThreeWayMerger merger = MergeStrategy.RECURSIVE.newMerger(repository, true);
				boolean success = merger.merge(targetCommit, sourceCommit);

				// extract the author from the source commit to be used in the merge outcome and the squash commit
				final PersonIdent author = sourceCommit.getAuthorIdent();

				if (success) {
					final CommitBuilder commit = new CommitBuilder();
					commit.setTreeId(merger.getResultTreeId());
					commit.setParentId(targetCommit);
					commit.setMessage(sourceCommit.getFullMessage());
					commit.setAuthor(author);
					commit.setCommitter(sourceCommit.getCommitterIdent());

					final ObjectId squashCommitId = inserter.insert(commit);
					inserter.flush();

					// Update the branch reference, this is the only real disk write
					final RefUpdate update = repository.updateRef(formatProfileRefName(profile));
					update.setNewObjectId(squashCommitId);
					update.setExpectedOldObjectId(targetCommit);

					final RefUpdate.Result result = update.update();

					switch (result) {
						case FAST_FORWARD, NO_CHANGE -> log.info(
								"Successfully applied changeset '{}' to profile '{}' of Service({}) with commit: {}",
								changeset, profile.slug(), service.id(), squashCommitId.name()
						);
						default -> throw createSourceControlExceptionForRefUpdateResult(update, result,
								"Failed to apply changeset '%s' to Git profile Ref '%s' owned by Service(%s, %s)"
										.formatted(changeset, profile.slug(), service.id(), service.slug())
						);
					}

					// remove the changeset branch once it is successfully applied...
					removeRef(repository, changeset);

					return MergeOutcome.applied(changeset, GitConverters.formatPerson(author), squashCommitId.name());
				} else if (merger instanceof ResolveMerger resolveMerger) {
					log.warn("Failed to apply changeset '{}' to profile '{}', manual resolution required.",
							changeset, profile.slug());

					return GitConverters.convertToConflictingOutcome(author, resolveMerger, changeset, profile.slug());
				} else {
					throw new RepositoryStateException(CONFLICT,
							"Failed to apply changeset '%s' to profile '%s' for Service(%s, %s)".formatted(
									changeset, profile.slug(), service.id(), service.slug())
					);
				}
			}
		});
	}

	@Override
	public void discard(Profile profile, String changeset) {
		executeRepositoryOperation(() -> {
			final Ref branch = repository.findRef(formatChangesetRefName(profile, changeset));

			if (branch == null) {
				throw new RepositoryStateException(UNKNOWN_CHANGESET,
						"Failed to discard changes from changeset '%s' of profile '%s' as does not exist for Service(%s, %s)"
								.formatted(changeset, profile.slug(), service.id(), service.slug()));
			}

			removeRef(repository, branch.getName());
			return Void.TYPE;
		});
	}

	@Override
	public void delete(Profile profile) {
		executeRepositoryOperation(() -> {
			final Ref branch = repository.findRef(formatProfileRefName(profile));

			if (branch == null) {
				throw new RepositoryStateException(UNKNOWN_PROFILE,
						"Failed to remove branch for profile with name '%s' as does not exist for Service(%s, %s)"
								.formatted(profile.slug(), service.id(), service.slug()));
			}

			removeRef(repository, branch.getName());

			log.info("Successfully deleted branch with ref '{}' from Git repository for {}", branch, service);

			return Void.TYPE;
		});
	}

	@Override
	public void destroy() {
		executeRepositoryOperation(() -> {
			final Path directory = repository.getDirectory().toPath();

			log.debug("Attempting to destroy Git repository for Service({}) under location: {}", service.id(), directory);

			try {
				FileUtils.deleteDirectory(directory.toFile());
			} catch (IOException ex) {
				throw new RepositoryStateException(UNAVAILABLE, "I/O failure while removing Git repository for Service(%s, %s)"
						.formatted(service.id(), service.slug()), ex);
			}

			log.info("Successfully destroyed Git repository for Service({}) under location: {}",
					service.id(), directory);

			return Void.TYPE;
		});
	}

	@Override
	public Page<RepositoryVersion> history(Profile profile, Pageable pageable) {
		return executeRepositoryOperation(() -> {
			final Ref branch = repository.findRef(formatProfileRefName(profile));

			if (branch == null) {
				throw new RepositoryStateException(UNKNOWN,
						"Failed to retrieve history for profile '%s' of the Service(%s, %s)"
								.formatted(profile.slug(), service.id(), service.slug()));
			}

			List<RepositoryVersion> result = new ArrayList<>();
			long total = 0;

			try (RevWalk walk = new RevWalk(repository)) {
				RevCommit head = walk.parseCommit(branch.getObjectId());
				walk.sort(RevSort.COMMIT_TIME_DESC);
				walk.markStart(head);

				int pageSize = pageable.getPageSize();
				int offset = (int) pageable.getOffset();
				int index = 0;

				for (RevCommit rev : walk) {
					if (index >= offset && result.size() < pageSize) {
						result.add(GitConverters.convertToRepositoryVersion(rev));
					}
					index++;
					total++;
				}
			}

			return new PageImpl<>(result, pageable, total);
		});
	}

	@Override
	public void close() {
		if (!closed) {
			closed = true;
			repository.close();
		}
	}

	@Override
	public String toString() {
		return "GitStateRepository(id=" + service.id() + ", name=" + service.slug()
				+ ", repository=" + repository.getIdentifier() + ")";
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof GitStateRepository that) {
			return Objects.equals(service, that.service);
		}
		return false;
	}

	@Override
	public int hashCode() {
		return service.hashCode();
	}

	private <T> T executeRepositoryOperation(ThrowingSupplier<T> operation) {
		if (closed) {
			throw new IllegalStateException("Can not perform operations on a closed repository: " + this);
		}

		try {
			return operation.getWithException();
		} catch (RepositoryStateException ex) {
			throw ex;
		} catch (RepositoryNotFoundException ex) {
			throw new RepositoryStateException(UNKNOWN_REPOSITORY, "Could not find repository for Service(%s, %s)"
					.formatted(service.id(), service.slug()), ex);
		} catch (CorruptObjectException ex) {
			throw new RepositoryStateException(INVALID_STATE, "The Git repository for Service(%s, %s) is corrupted"
					.formatted(service.id(), service.slug()), ex);
		} catch (IndexReadException | IndexWriteException ex) {
			throw new RepositoryStateException(INVALID_STATE, "The Git repository for Service(%s, %s) can not manage index files"
					.formatted(service.id(), service.slug()), ex);
		} catch (IOException ex) {
			throw new RepositoryStateException(UNAVAILABLE,
					"I/O failure while accessing or executing Git repository operation for Service(%s, %s)"
							.formatted(service.id(), service.slug()), ex);
		} catch (Exception ex) {
			throw new RepositoryStateException(UNAVAILABLE,
					"Unexpected error occurred while executing Git repository operation for Service(%s, %s)"
							.formatted(service.id(), service.slug()), ex);
		}
	}

	private RepositoryState loadState(ObjectId branchId) throws IOException {
		// The most optimal way to read file contents is to read the file contents is to pull it
		// directly from the Git Object database and avoid the physical disk. For this we need to
		// walk through the current repository using the resolved changeset branch and the latest commit
		try (RevWalk walker = new RevWalk(repository)) {
			final RevCommit commit = walker.parseCommit(branchId);

			// Use a TreeWalk to navigate the tree of that commit and find our configuration state file
			try (TreeWalk tree = new TreeWalk(repository)) {
				tree.addTree(commit.getTree());
				tree.setRecursive(false);
				tree.setFilter(CONFIGURATION_STATE_PATH_FILTER);

				// the file is not found, this is because the profile branch was just created, and it
				// contains no configuration state yet. If this was the changeset branch, it was most
				// likely created from this 'clean' profile branch. In this case we should return an
				// empty string and let the user create his first configuration state.
				if (!tree.next()) {
					return GitConverters.convertToRepositoryState(commit, InputStream::nullInputStream);
				}

				// load the file contents and return it as an input stream
				final ObjectId objectId = tree.getObjectId(0);
				final ObjectLoader loader = repository.open(objectId);

				return GitConverters.convertToRepositoryState(commit, loader::openStream);
			}
		}
	}

	private String createChangesetBranchForProfile(Profile profile, ObjectId profileObjectId) throws IOException {
		log.debug("Attempting to create a new changeset branch for profile '{}' for Service({})", profile, service.id());

		// We need to create the RefUpdate with the full reference name (e.g., refs/heads/profile/changeset/uuid)
		final String changesetName = formatChangesetRefName(profile, UUID.randomUUID().toString());
		final RefUpdate update = repository.updateRef(changesetName);

		// Now we need to set the destination hash, the profile...
		update.setNewObjectId(profileObjectId);

		// get the result for the operation and evaluate it...
		final RefUpdate.Result result = update.update();

		switch (result) {
			case NEW, FAST_FORWARD, NO_CHANGE, RENAMED -> {
				log.info("Successfully created a new changeset branch for profile with name '{}' for Service({})",
						profile, service.id());
			}
			default -> throw createSourceControlExceptionForRefUpdateResult(update, result,
					"Failed to create Git ref for changeset for profile '%s' owned by Service(%s, %s)".formatted(
							profile, service.id(), service.slug())
			);
		}

		return changesetName;
	}

	private static String formatProfileRefName(Profile profile) {
		return Constants.R_HEADS + "profile/" + profile.slug();
	}

	private static String formatChangesetRefName(Profile profile, String changeset) {
		if (changeset.startsWith(Constants.R_HEADS)) {
			return changeset;
		}
		return Constants.R_HEADS + "changeset/" + profile.slug() + "/" + changeset;
	}

	private static Path createRepositoryLocation(Path parent, Service service) {
		return parent.resolve("service-repository-%s".formatted(service.id().serialize()));
	}

	private static void removeRef(Repository repository, String branch) throws IOException {
		// Create the update object for the given reference name/path
		final RefUpdate update = repository.updateRef(branch);
		// Use force to allow deleting a branch even if it's the current HEAD
		update.setForceUpdate(true);

		final RefUpdate.Result result = update.delete();

		switch (result) {
			case FORCED, FAST_FORWARD, NO_CHANGE:
				break;
			default:
				throw new IllegalStateException("Failed to delete branch with ref '%s', delete result was '%s'"
						.formatted(branch, result));
		}
	}

	private static RepositoryStateException createSourceControlExceptionForRefUpdateResult(
			RefUpdate update, RefUpdate.Result result, String message
	) {
		switch (result) {
			case FORCED -> throw new RepositoryStateException(CONFLICT,
					"%s. Git 'force' is not permitted when interacting with Git ref when name '%s'."
							.formatted(message, update.getName())
			);
			case LOCK_FAILURE, REJECTED -> throw new RepositoryStateException(CONFLICT,
					"%s. Concurrent modification detected when updating the Git ref with name '%s'."
							.formatted(message, update.getName())
			);
			case REJECTED_MISSING_OBJECT -> throw new RepositoryStateException(INVALID_STATE,
					"%s. Failed to perform update on a Git ref with name '%s' as it is corrupted or missing objects"
							.formatted(message, update.getName())
			);
			case REJECTED_CURRENT_BRANCH, NOT_ATTEMPTED -> throw new RepositoryStateException(INVALID_STATE,
					"%s. Unsupported operation performed on a Git ref with name '%s'."
							.formatted(message, update.getName())
			);
			case IO_FAILURE -> throw new RepositoryStateException(UNAVAILABLE,
					"%s. I/O failure detected when performing update on Ref with name '%s'"
							.formatted(message, update.getName())
			);
			case REJECTED_OTHER_REASON -> throw new RepositoryStateException(UNKNOWN,
					"%s. Operation performed on a Git ref with name '%s' was rejected for an unknown reason."
							.formatted(message, update.getName())
			);
			default -> throw new RepositoryStateException(INVALID_STATE,
					"%s. Unexpected operation result state of '%s' when updating Ref with name '%s'."
							.formatted(message, result, update.getName())
			);
		}
	}

}
