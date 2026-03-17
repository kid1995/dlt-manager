package de.signaliduna.dltmanager.test;

public enum ContainerImageNames {
    POSTGRES("hub.docker.system.local/postgres:15-alpine");

    private final String imageName;

    ContainerImageNames(String imageName) {
        this.imageName = imageName;
    }

    public String getImageName() {
        return imageName;
    }
}
