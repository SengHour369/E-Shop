package com.example.eshop.ai;
import com.example.eshop.ai.service.AiTrainingExampleService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;
class AiTrainingRetrievalTest {
 @Test void disabledRetrievalDoesNotRequireStorage(){assertThat(new AiTrainingExampleService(null,false).retrieve("stock","en")).isEmpty();}
 @Test void knowledgeToolUsesReviewedSeedGuidanceWithoutVectorEmbeddingConfiguration(){
  var examples=org.mockito.Mockito.mock(AiTrainingExampleService.class);
  org.mockito.Mockito.when(examples.retrieve("stock","en")).thenReturn(java.util.List.of(java.util.Map.of("prompt","Stock guidance","response","Check real inventory first.")));
  var knowledge=new com.example.eshop.ai.service.AiKnowledgeService(null,new com.fasterxml.jackson.databind.ObjectMapper(),null,null,false,"","",examples);
  var result=knowledge.retrieve("stock");assertThat(result.path("answer").asText()).isEqualTo("Check real inventory first.");assertThat(result.path("referenceOnly").asBoolean()).isTrue();
 }
 @Test @EnabledIfEnvironmentVariable(named="AI_TRAINING_TEST_URL",matches=".+")
 void onlyApprovedConsentedVerifiedTrainExamplesOfRequestedLanguageReachRuntime(){
  var data=new DriverManagerDataSource(System.getenv("AI_TRAINING_TEST_URL"),System.getenv("AI_TRAINING_TEST_USERNAME"),System.getenv("AI_TRAINING_TEST_PASSWORD"));
  var jdbc=new JdbcTemplate(data);var tx=new TransactionTemplate(new DataSourceTransactionManager(data));
  tx.execute(status->{status.setRollbackOnly();UUID dataset=UUID.randomUUID();String marker="retrievalmarker"+UUID.randomUUID().toString().replace("-","");
   jdbc.update("INSERT INTO ai_training.datasets(id,code,name,created_by) VALUES(?,?,?,?)",dataset,"test-"+dataset,"isolated test","test");
   for(String[] row:new String[][]{{"APPROVED","TRAIN","en","approved guidance"},{"DRAFT","TRAIN","en","draft excluded"},{"APPROVED","VALIDATION","en","validation excluded"},{"APPROVED","TEST","en","test excluded"},{"APPROVED","TRAIN","km","other language excluded"}})
    jdbc.update("INSERT INTO ai_training.examples(dataset_id,language,prompt,response,source,created_by,split,source_verified,consent_confirmed,review_status,reviewed_by,reviewed_at) VALUES(?,?,?,?,?,'test',?,true,true,?,'automated-test',now())",dataset,row[2],marker,row[3],"synthetic:test",row[1],row[0]);
   var result=new AiTrainingExampleService(jdbc,true).retrieve(marker,"en");assertThat(result).hasSize(1);assertThat(result.get(0).get("response")).isEqualTo("approved guidance");
   assertThat(new AiTrainingExampleService(jdbc,true).retrieve(marker+" នៅ","en").get(0).get("response")).isEqualTo("other language excluded");
   jdbc.update("UPDATE ai_training.examples SET review_status='REJECTED' WHERE dataset_id=? AND response='approved guidance'",dataset);
   assertThat(new AiTrainingExampleService(jdbc,true).retrieve(marker,"en")).isEmpty();return null;
  });
 }
}
