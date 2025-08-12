package ru.rudn.rudnadmin.service.minio;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import ru.rudn.rudnadmin.config.TestContainers;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static ru.rudn.rudnadmin.testutil.TestDataUtils.getVpnFilePath;

@SpringBootTest
class MinioServiceImplTest extends TestContainers {

    @Autowired
    private MinioService minioService;

    @Test
    void uploadGetPresignedAndDelete() throws Exception {
        final String objectName = "it-some-connect.ovpn";

        minioService.load(objectName, getVpnFilePath());
        final String url = minioService.getLink(objectName);
        Assertions.assertNotNull(url);
        Assertions.assertTrue(url.contains(objectName));

        final RestTemplate rt = new RestTemplate(new SimpleClientHttpRequestFactory());
        final var entity = rt.getForEntity(URI.create(url), byte[].class);
        Assertions.assertEquals(HttpStatus.OK, HttpStatus.valueOf(entity.getStatusCode().value()));
        Assertions.assertTrue(entity.getBody().length > 0);

        minioService.delete(objectName);
        try {
            rt.getForEntity(URI.create(url), byte[].class);
        } catch (HttpClientErrorException e) {
            Assertions.assertEquals(HttpStatus.NOT_FOUND, e.getStatusCode());
        }
    }

    @Test
    @DisplayName("Если ссылка истекла - генерится новая ссылка, которая доступна")
    void uploadGetPresignedAndRenewLink() throws Exception {
        final String objectName = "it-some-connect.ovpn";
        minioService.load(objectName, getVpnFilePath());

        final String url1 = minioService.getLink(objectName);
        Assertions.assertNotNull(url1);
        Assertions.assertTrue(url1.contains(objectName));

        // чтобы ссылка изменилась, т.к. она генерируется исходя из времени
        Thread.sleep(2000);

        final String url2 = minioService.getLink(objectName);
        Assertions.assertNotNull(url2);
        Assertions.assertTrue(url2.contains(objectName));

        Assertions.assertNotEquals(url1, url2);

        // url2 работает и всё ок
        final RestTemplate rt = new RestTemplate(new SimpleClientHttpRequestFactory());
        final var entity = rt.getForEntity(URI.create(url2), byte[].class);
        Assertions.assertEquals(HttpStatus.OK, HttpStatus.valueOf(entity.getStatusCode().value()));
        Assertions.assertTrue(entity.getBody().length > 0);
    }

    @Test
    @DisplayName("Повторная загрузка файла с тем же именем перезаписывает содержимое объекта")
    void uploadSameName_overwritesObject() throws Exception {
        final String objectName = "it-same-name.ovpn";

        // 1) Первая загрузка исходного файла из ресурсов
        minioService.load(objectName, getVpnFilePath());

        final RestTemplate rt = new RestTemplate(new SimpleClientHttpRequestFactory());
        final String urlV1 = minioService.getLink(objectName);
        final var respV1 = rt.getForEntity(URI.create(urlV1), byte[].class);
        Assertions.assertEquals(HttpStatus.OK, HttpStatus.valueOf(respV1.getStatusCode().value()));
        final byte[] bodyV1 = respV1.getBody();
        Assertions.assertTrue(bodyV1.length > 0);

        // 2) Вторая загрузка — другой файл с тем же именем (другая длина/содержимое)
        final Path tmpV2 = Files.createTempFile("minio-overwrite-", ".ovpn");
        final String differentContent = "DIFFERENT CONTENT " + System.nanoTime();
        Files.write(tmpV2, differentContent.getBytes(StandardCharsets.UTF_8));
        minioService.load(objectName, tmpV2.toAbsolutePath().toString());

        // 3) Получаем новую ссылку и убеждаемся, что содержимое изменилось
        final String urlV2 = minioService.getLink(objectName);
        final var respV2 = rt.getForEntity(URI.create(urlV2), byte[].class);
        Assertions.assertEquals(HttpStatus.OK, HttpStatus.valueOf(respV2.getStatusCode().value()));
        final byte[] bodyV2 = respV2.getBody();

        Assertions.assertNotEquals(bodyV1.length, bodyV2.length);
        Assertions.assertEquals(Files.size(tmpV2), bodyV2.length);

    }
}


