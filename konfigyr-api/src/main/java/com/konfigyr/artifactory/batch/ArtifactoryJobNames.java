package com.konfigyr.artifactory.batch;

@SuppressWarnings("InterfaceIsType")
public interface ArtifactoryJobNames {

	/**
	 * The name of the Artifactory publish {@link org.springframework.batch.core.job.Job} that
	 * is executed when a new {@link com.konfigyr.artifactory.Publication} is uploaded.
	 */
	String PUBLISH_JOB = "konfigyr.artifactory.publish-component";

}
