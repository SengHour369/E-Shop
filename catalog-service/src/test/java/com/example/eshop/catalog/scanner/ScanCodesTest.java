package com.example.eshop.catalog.scanner;

import com.example.eshop.common.exception.BusinessLogicException;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ScanCodesTest {
    @Test void acceptsValidEan13AndRejectsABadCheckDigit() {
        var code = ScanCodes.normalize("8850 1234 56787", ScanFormat.EAN_13);
        assertThat(code.code()).isEqualTo("8850123456787");
        assertThat(code.barcodeCandidates()).containsExactly("8850123456787");
        assertThat(code.skuCandidates()).isEmpty();
        assertThatThrownBy(() -> ScanCodes.normalize("8850123456789", ScanFormat.EAN_13))
                .isInstanceOf(BusinessLogicException.class)
                .hasMessage("Barcode check digit is invalid");
    }

    @Test void acceptsUpcAAndItsEan13Equivalent() {
        var code = ScanCodes.normalize("036000291452", null);
        assertThat(code.format()).isEqualTo(ScanFormat.UPC_A);
        assertThat(code.barcodeCandidates()).containsExactly("036000291452", "0036000291452");
    }

    @Test void acceptsEan8() {
        var code = ScanCodes.normalize("96385074", ScanFormat.EAN_8);
        assertThat(code.code()).isEqualTo("96385074");
        assertThat(code.skuCandidates()).isEmpty();
    }

    @Test void expandsUpcEToUpcA() {
        assertThat(ScanCodes.expandUpcE("04252614")).isEqualTo("042100005264");
        var code = ScanCodes.normalize("04252614", ScanFormat.UPC_E);
        assertThat(code.barcodeCandidates()).contains("04252614", "042100005264", "0042100005264");
    }

    @Test void qrAndSkuStayDistinctFromTheRetailBarcode() {
        var qr = ScanCodes.normalize("https://shop.example/p/NIKE-BLK-42", ScanFormat.QR_CODE);
        assertThat(qr.skuCandidates()).contains("NIKE-BLK-42");
        assertThat(qr.barcodeCandidates()).contains("NIKE-BLK-42");
        var sku = ScanCodes.normalize("nike-blk-42", ScanFormat.SKU);
        assertThat(sku.code()).isEqualTo("NIKE-BLK-42");
        assertThat(sku.skuCandidates()).containsExactly("NIKE-BLK-42");
    }

    @Test void code39AndCode128AcceptTheirAlphabets() {
        assertThat(ScanCodes.normalize("ABC-123", ScanFormat.CODE_39).code()).isEqualTo("ABC-123");
        assertThat(ScanCodes.normalize("SKU.100", ScanFormat.CODE_128).skuCandidates()).contains("SKU.100");
    }

    @Test void rejectsEmptyControlAndUnknownInput() {
        assertThatThrownBy(() -> ScanCodes.normalize("  ", ScanFormat.EAN_13))
                .isInstanceOf(BusinessLogicException.class);
        assertThatThrownBy(() -> ScanCodes.normalize("8850123456787\n", null))
                .isInstanceOf(BusinessLogicException.class)
                .hasMessage("Scanner code contains unsupported characters");
        assertThatThrownBy(() -> ScanCodes.normalize("123", ScanFormat.EAN_13))
                .isInstanceOf(BusinessLogicException.class);
        assertThat(ScanCodes.storedBarcode("  ")).isNull();
        assertThat(ScanCodes.storedBarcode("8850123456787")).isEqualTo("8850123456787");
        assertThatThrownBy(() -> ScanCodes.storedBarcode("bad code!"))
                .isInstanceOf(BusinessLogicException.class);
    }
}
