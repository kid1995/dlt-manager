package de.signaliduna.dltmanager.test;

public enum ContainerImageNames {
    POSTGRES("postgres:16-alpine");

    private final String imageName;

    ContainerImageNames(String imageName) {
        this.imageName = imageName;
    }

    public String getImageName() {
        return imageName;
    }
}
