package com.example.grpcdemo.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

import java.net.URI;

/**
 * Configuration for interacting with Supabase object storage.
 */
@ConfigurationProperties(prefix = "supabase")
public class SupabaseStorageProperties {

    private URI projectUrl = URI.create("https://project.supabase.co");
    private String serviceRoleKey = "";

    @NestedConfigurationProperty
    private final Storage storage = new Storage();

    public URI getProjectUrl() {
        return projectUrl;
    }

    public void setProjectUrl(URI projectUrl) {
        this.projectUrl = projectUrl;
    }

    public String getServiceRoleKey() {
        return serviceRoleKey;
    }

    public void setServiceRoleKey(String serviceRoleKey) {
        this.serviceRoleKey = serviceRoleKey;
    }

    public Storage getStorage() {
        return storage;
    }

    public URI resolveApiBaseUrl() {
        String base = projectUrl.toString();
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return URI.create(base + "/storage/v1");
    }

    public static class Storage {

        private URI publicUrl;

        @NestedConfigurationProperty
        private Bucket jobDocuments = new Bucket("job-documents", "job-documents");

        @NestedConfigurationProperty
        private Bucket resumes = new Bucket("resumes", "resumes");

        @NestedConfigurationProperty
        private Bucket interviewAudios = new Bucket("interview-audios", "interview-audios");

        @NestedConfigurationProperty
        private Bucket profilePhotos = new Bucket("profile-photos", "profile-photos");

        public URI getPublicUrl() {
            return publicUrl;
        }

        public void setPublicUrl(URI publicUrl) {
            this.publicUrl = publicUrl;
        }

        public Bucket getJobDocuments() {
            return jobDocuments;
        }

        public void setJobDocuments(Bucket jobDocuments) {
            this.jobDocuments = jobDocuments;
        }

        public Bucket getResumes() {
            return resumes;
        }

        public void setResumes(Bucket resumes) {
            this.resumes = resumes;
        }

        public Bucket getInterviewAudios() {
            return interviewAudios;
        }

        public void setInterviewAudios(Bucket interviewAudios) {
            this.interviewAudios = interviewAudios;
        }

        public Bucket getProfilePhotos() {
            return profilePhotos;
        }

        public void setProfilePhotos(Bucket profilePhotos) {
            this.profilePhotos = profilePhotos;
        }
    }

    public static class Bucket {

        private String name;
        private String pathPrefix;

        public Bucket() {
        }

        public Bucket(String name, String pathPrefix) {
            this.name = name;
            this.pathPrefix = pathPrefix;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getPathPrefix() {
            return pathPrefix;
        }

        public void setPathPrefix(String pathPrefix) {
            this.pathPrefix = pathPrefix;
        }
    }
}

