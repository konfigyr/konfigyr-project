package com.konfigyr.artifactory.batch;

public interface ArtifactoryJobNames {

	/**
	 * The name of the Artifactory release {@link org.springframework.batch.core.Job} that
	 * is executed when a new {@link com.konfigyr.artifactory.Component} is uploaded.
	 */
	String RELEASE_JOB = "konfigyr.artifactory.release-component";

}
