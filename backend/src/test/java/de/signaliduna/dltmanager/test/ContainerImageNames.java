package de.signaliduna.dltmanager.test;

import org.testcontainers.utility.DockerImageName;

public enum ContainerImageNames {
	MONGO(DockerImageName.parse("mongo:6").asCompatibleSubstituteFor("mongo"));
	private final DockerImageName imageName;

	ContainerImageNames(DockerImageName imageName) {
		this.imageName = imageName;
	}

	public DockerImageName getImageName() {
		return imageName;
	}
}
