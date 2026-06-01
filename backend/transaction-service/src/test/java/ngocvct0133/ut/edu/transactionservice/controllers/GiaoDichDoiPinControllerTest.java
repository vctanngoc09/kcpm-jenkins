package ngocvct0133.ut.edu.transactionservice.controllers;

import ngocvct0133.ut.edu.transactionservice.services.IGiaoDichDoiPinService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(GiaoDichDoiPinController.class)
class GiaoDichDoiPinControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private IGiaoDichDoiPinService giaoDichDoiPinService;

    @Test
    void createTransactionWithEmptyBodyShouldReturn400() throws Exception {
        mockMvc.perform(post("/api/transaction-service/giaodichdoipin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createTransactionWithInvalidStatusShouldReturn400() throws Exception {
        mockMvc.perform(post("/api/transaction-service/giaodichdoipin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "maPinTra": "PIN-001",
                                  "maPinNhan": "PIN-002",
                                  "ngayGiaoDich": "2026-05-29T10:15:30",
                                  "trangThaiGiaoDich": "INVALID_STATUS",
                                  "thanhtien": 1200000,
                                  "phuongThucThanhToan": "cash",
                                  "maTram": 1,
                                  "maTaiXe": 2
                                }
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(giaoDichDoiPinService);
    }

    @Test
    void createTransactionWithMissingAmountShouldReturn400() throws Exception {
        mockMvc.perform(post("/api/transaction-service/giaodichdoipin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "maPinTra": "PIN-001",
                                  "maPinNhan": "PIN-002",
                                  "ngayGiaoDich": "2026-05-29T10:15:30",
                                  "trangThaiGiaoDich": "Đã hoàn thành",
                                  "phuongThucThanhToan": "cash",
                                  "maTram": 1,
                                  "maTaiXe": 2
                                }
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(giaoDichDoiPinService);
    }

      @Test
      void createTransactionWithInvalidPaymentMethodShouldReturn400() throws Exception {
        mockMvc.perform(post("/api/transaction-service/giaodichdoipin")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "maPinTra": "PIN-001",
                      "maPinNhan": "PIN-002",
                      "ngayGiaoDich": "2026-05-29T10:15:30",
                      "trangThaiGiaoDich": "Đã hoàn thành",
                      "thanhtien": 1200000,
                      "phuongThucThanhToan": "bitcoin",
                      "maTram": 1,
                      "maTaiXe": 2
                    }
                    """))
            .andExpect(status().isBadRequest());

        verifyNoInteractions(giaoDichDoiPinService);
      }
}