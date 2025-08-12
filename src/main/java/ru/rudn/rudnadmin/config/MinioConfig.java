package ru.rudn.rudnadmin.config;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MinioConfig {

    @Value("${minio.url}")
    private String url;

    @Value("${minio.access-key}")
    private String username;

    @Value("${minio.secret-key}")
    private String password;

    @Value("${minio.bucket.name}")
    private String vpnBucket;

    @Bean
    public MinioClient minioClient() {
        return MinioClient.builder()
                .endpoint(url)
                .credentials(username, password)
                .build();
    }

    @Bean
    public BeanPostProcessor minioBucketInitializer() {
        return new BeanPostProcessor() {

            @NotNull
            @Override
            public Object postProcessAfterInitialization(@NotNull Object bean, @NotNull String beanName) throws BeansException {
                if (bean instanceof MinioClient minioClient) {
                    try {
                        if (!minioClient.bucketExists(BucketExistsArgs.builder().bucket(vpnBucket).build())) {
                            minioClient.makeBucket(MakeBucketArgs.builder().bucket(vpnBucket).build());
                        }
                    } catch (Exception e) {
                        throw new RuntimeException("Cannot check/initialize bucket " + vpnBucket, e);
                    }
                }

                return bean;
            }
        };
    }
}
