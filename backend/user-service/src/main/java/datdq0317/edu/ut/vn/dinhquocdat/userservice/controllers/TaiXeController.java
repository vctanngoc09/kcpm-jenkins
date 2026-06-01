package datdq0317.edu.ut.vn.dinhquocdat.userservice.controllers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import datdq0317.edu.ut.vn.dinhquocdat.userservice.dtos.TaiXeDTO;
import datdq0317.edu.ut.vn.dinhquocdat.userservice.dtos.TaiXeResponse;
import datdq0317.edu.ut.vn.dinhquocdat.userservice.models.TaiXe;
import datdq0317.edu.ut.vn.dinhquocdat.userservice.services.ITaiXeService;
@RestController
@RequestMapping("/api/user-service/taixe")
public class TaiXeController {

    @Autowired
    private ITaiXeService taiXeService;

    @PostMapping
    public ResponseEntity<TaiXe> themTaiXe(@RequestBody TaiXeDTO dto) {
        TaiXe tx = taiXeService.themTaiXe(dto);
        return ResponseEntity.ok(tx);
    }

    @GetMapping
    public ResponseEntity<List<TaiXe>> danhSachTaiXe() {
        return ResponseEntity.ok(taiXeService.danhSachTaiXe());
    }

        // GET BY ID
    @GetMapping("/{id}")
    public ResponseEntity<?> layTaiXeTheoId(@PathVariable Long id) {
        TaiXe tx = taiXeService.layTaiXeTheoId(id);

        if (tx == null) {
            return ResponseEntity.status(404).body(
                Map.of("message", "Người dùng không tồn tại")
            );
        }

        return ResponseEntity.ok(tx);
    }

   // GET BY USER ID
    @GetMapping("/user/{id}")
    public ResponseEntity<?> layTaiXeTheoMaNguoiDung(@PathVariable Long id) {
        try {
            TaiXe tx = taiXeService.layTaiXeTheoMaNguoiDung(id);

            if (tx == null) {
                return ResponseEntity.status(404).body(
                    Map.of("message", "Người dùng không tồn tại")
                );
            }

            return ResponseEntity.ok(tx);

        } catch (Exception e) {
            return ResponseEntity.status(500).body(
                Map.of("message", "Lỗi hệ thống")
            );
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> suaTaiXe(@PathVariable Long id, @RequestBody TaiXeDTO dto) {
    try {
        TaiXe updated = taiXeService.suaTaiXe(id, dto);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Cập nhật thông tin tài xế thành công");

        // giữ structure cũ
        response.put("maTaiXe", updated.getMaTaiXe());
        response.put("bangLaiXe", updated.getBangLaiXe());
        response.put("nguoiDung", updated.getNguoiDung());

        return ResponseEntity.ok(response);

    } catch (RuntimeException e) {

    Map<String, String> error = new HashMap<>();
    error.put("message", e.getMessage());

    return ResponseEntity.status(404).body(error);
}
}

   @DeleteMapping("/{id}")
public ResponseEntity<?> xoaTaiXe(@PathVariable Long id) {
    try {

        boolean deleted = taiXeService.xoaTaiXe(id);

        if (deleted) {
            return ResponseEntity.ok(
                Map.of("message", "Xóa tài xế thành công")
            );
        }

        return ResponseEntity.status(404).body(
            Map.of("message", "Không tìm thấy tài xế!")
        );

    } catch (Exception e) {

        return ResponseEntity.status(404).body(
            Map.of("message", e.getMessage())
        );
    }
}
@GetMapping("/info/{id}")
public ResponseEntity<TaiXeResponse> layThongTinTaiXe(@PathVariable Long id) {
    TaiXeResponse response = taiXeService.layThongTinTaiXe(id);
    return ResponseEntity.ok(response);
}

}
