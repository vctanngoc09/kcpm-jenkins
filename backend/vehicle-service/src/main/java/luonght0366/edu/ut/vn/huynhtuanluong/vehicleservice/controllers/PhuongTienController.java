package luonght0366.edu.ut.vn.huynhtuanluong.vehicleservice.controllers;

import luonght0366.edu.ut.vn.huynhtuanluong.vehicleservice.dtos.PhuongTienDTO;
import luonght0366.edu.ut.vn.huynhtuanluong.vehicleservice.modules.PhuongTien;
import luonght0366.edu.ut.vn.huynhtuanluong.vehicleservice.services.IPhuongTienService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/vehicle-service/vehicles")
public class PhuongTienController {

    @Autowired
    private IPhuongTienService phuongTienService;

    @PostMapping
    public ResponseEntity<?> themPhuongTien(@RequestBody PhuongTienDTO dto) {
        try {
            PhuongTien v = phuongTienService.themPhuongTien(dto);
            return ResponseEntity
                    .created(URI.create("/api/vehicle-service/vehicles/" + v.getMaPhuongTien()))
                    .body(v);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(error(e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<List<PhuongTien>> danhSachPhuongTien() {
        return ResponseEntity.ok(phuongTienService.danhSachPhuongTien());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> layPhuongTienTheoId(@PathVariable Long id) {
        PhuongTien v = phuongTienService.layPhuongTienTheoId(id);

        if (v == null) {
            return ResponseEntity.status(404).body(error("Không tìm thấy phương tiện"));
        }

        return ResponseEntity.ok(v);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> suaPhuongTien(@PathVariable Long id, @RequestBody PhuongTienDTO dto) {
        try {
            PhuongTien updated = phuongTienService.suaPhuongTien(id, dto);
            return ResponseEntity.ok(updated);
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(404).body(error(e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(error(e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> xoaPhuongTien(@PathVariable Long id) {
        boolean deleted = phuongTienService.xoaPhuongTien(id);

        if (!deleted) {
            return ResponseEntity.status(404).body(error("Không tìm thấy phương tiện"));
        }

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/by-driver/{driverId}")
    public ResponseEntity<List<PhuongTien>> danhSachTheoTaiXe(@PathVariable("driverId") Long driverId) {
        return ResponseEntity.ok(phuongTienService.danhSachTheoTaiXe(driverId));
    }

    @PostMapping("/{id}/link-pin/{pinId}")
    public ResponseEntity<?> lienKetPin(@PathVariable Long id, @PathVariable("pinId") Long pinId) {
        try {
            return ResponseEntity.ok(phuongTienService.lienKetPin(id, pinId));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(404).body(error(e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(error(e.getMessage()));
        }
    }

    @PostMapping("/{id}/unlink-pin")
    public ResponseEntity<?> huyLienKetPin(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(phuongTienService.huyLienKetPin(id));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(404).body(error(e.getMessage()));
        }
    }

    private Map<String, String> error(String message) {
        return Map.of("error", message);
    }
}