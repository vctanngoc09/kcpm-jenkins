package ut.edu.batteryservice.services;

import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import ut.edu.batteryservice.models.LichSuPinTram;
import ut.edu.batteryservice.models.Pin;
import ut.edu.batteryservice.repositories.ILichSuPinTramRepository;

import java.util.List;

@Service
public class LichSuPinTramService implements ILichSuPinTramService {

    @Autowired
    private ILichSuPinTramRepository lichSuPinTramRepository;

    @Override
    public List<LichSuPinTram> findAll() {
        return lichSuPinTramRepository.findAll();
    }

    @Override
    public LichSuPinTram findById(Long id) {
        return lichSuPinTramRepository.findById(id).orElse(null);
    }

    @Transactional
    @Override
    public LichSuPinTram save(LichSuPinTram lichSuPinTram) {
        validateLichSuPinTram(lichSuPinTram);
        return lichSuPinTramRepository.save(lichSuPinTram);
    }

    @Transactional
    @Override
    public boolean deleteById(Long id) {
        if (!lichSuPinTramRepository.existsById(id)) {
            return false;
        }
        lichSuPinTramRepository.deleteById(id);
        return true;
    }

    @Transactional
    @Override
    public LichSuPinTram addLichSuPinTram(LichSuPinTram lichSuPinTram) {
        validateLichSuPinTram(lichSuPinTram);
        return lichSuPinTramRepository.save(lichSuPinTram);
    }

    @Override
    public List<Pin> getAvailablePins(Long maTram, String loaiPin) {
        return lichSuPinTramRepository.findAvailablePinsByTramAndLoai(maTram, loaiPin);
    }

    private void validateLichSuPinTram(LichSuPinTram lichSuPinTram) {
        if (lichSuPinTram == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Dữ liệu lịch sử pin trạm không được rỗng");
        }

        if (lichSuPinTram.getHanhDong() == null || lichSuPinTram.getHanhDong().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Hành động không được để trống");
        }

        if (lichSuPinTram.getMaPin() == null || lichSuPinTram.getMaPin() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mã pin phải lớn hơn 0");
        }

        if (lichSuPinTram.getMaTram() == null || lichSuPinTram.getMaTram() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mã trạm phải lớn hơn 0");
        }
    }
}