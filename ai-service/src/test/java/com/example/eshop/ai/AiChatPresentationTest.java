package com.example.eshop.ai;

import com.example.eshop.ai.dto.AiResponse;
import com.example.eshop.ai.enums.AiExecutionStatus;
import com.example.eshop.ai.enums.AiIntent;
import com.example.eshop.ai.service.AiChatPresenter;
import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;

class AiChatPresentationTest {
    private final AiChatPresenter presenter = new AiChatPresenter();
    private AiResponse result(AiExecutionStatus status) {
        return new AiResponse(null,null,null,AiIntent.ADMIN_ORDER_LIST,status,"Orders",null,null);
    }
    @Test void successfulAdminOrderQuerySelectsNativeViewInBothLanguages() {
        for (String language : new String[]{"en","km"}) {
            var response = presenter.present(UUID.randomUUID(),true,language,result(AiExecutionStatus.SUCCESS));
            assertThat(response.presentation().schemaVersion()).isEqualTo("1.0");
            assertThat(response.presentation().blocks()).singleElement().satisfies(block -> assertThat(block.kind()).isEqualTo("orders.list"));
        }
    }
    @Test void inventoryViewsAreOnlySelectedForSuccessfulAdministrators() {
        for (AiIntent intent : new AiIntent[]{AiIntent.INVENTORY_GET,AiIntent.INVENTORY_LOW_STOCK}) {
            var success = new AiResponse(null,null,null,intent,AiExecutionStatus.SUCCESS,"Inventory",null,null);
            var response = presenter.present(UUID.randomUUID(),true,"en",success);
            assertThat(response.presentation().blocks().get(0).kind()).isEqualTo(intent == AiIntent.INVENTORY_GET ? "inventory.list" : "inventory.low-stock");
            assertThat(presenter.present(UUID.randomUUID(),false,"en",success).presentation()).isNull();
        }
    }
    @Test void customerAndFailedQueriesNeverSelectAdminView() {
        assertThat(presenter.present(UUID.randomUUID(),false,"en",result(AiExecutionStatus.SUCCESS)).presentation()).isNull();
        assertThat(presenter.present(UUID.randomUUID(),true,"en",result(AiExecutionStatus.FAILURE)).presentation()).isNull();
    }
}
