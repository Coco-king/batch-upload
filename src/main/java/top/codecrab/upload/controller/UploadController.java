package top.codecrab.upload.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import top.codecrab.upload.utils.AliyunUpload;
import top.codecrab.upload.utils.Times;
import top.codecrab.upload.utils.UploadUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * @author codecrab
 * @since 2021年08月21日 下午 03:37
 */
@RestController
@RequestMapping("/upload")
public class UploadController {

    @PostMapping("/single")
    public ResponseEntity<List<String>> singleUpload(MultipartFile[] files) {
        List<String> result = new ArrayList<>();
        Times.test("单线程上传耗时测试", () -> {
            for (MultipartFile file : files) {
                try {
                    String call = new AliyunUpload(file).call();
                    result.add(call);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        return ResponseEntity.ok(result);
    }

    @PostMapping("/multi")
    public ResponseEntity<List<String>> batchUpload(MultipartFile[] files) {
        List<String> result = new ArrayList<>();
        Times.test("多线程上传耗时测试", () -> {
            result.addAll(UploadUtil.multithreadedUpload(files));
        });
        return ResponseEntity.ok(result);
    }
}
