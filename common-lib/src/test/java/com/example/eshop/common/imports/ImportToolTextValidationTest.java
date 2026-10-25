package com.example.eshop.common.imports;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
class ImportToolTextValidationTest {
 Map<String,String> product(String description){var values=new HashMap<String,String>();values.put("name","Product");values.put("imageIds","00000000-0000-4000-8000-000000000001");values.put("imageChecksums","a".repeat(64));values.put("subCategoryId","1");values.put("isActive","true");values.put("description",description);return values;}
 @Test void acceptsMultilineKhmerDescriptionsWithoutChangingThem(){var text="ការពិពណ៌នា\nLine two\r\nLine three\tDetails";var values=product(text);assertThat(ImportTool.PRODUCT_BULK_CREATE.validate(values)).isEmpty();assertThat(values.get("description")).isEqualTo(text);}
 @Test void rejectsUnsafeControlCharacters(){assertThat(ImportTool.PRODUCT_BULK_CREATE.validate(product("Text\u0000hidden"))).contains("INVALID_VALUE: description");}
 @Test void namesRemainSingleLine(){var values=product("Valid description");values.put("name","Product\nOther");assertThat(ImportTool.PRODUCT_BULK_CREATE.validate(values)).contains("INVALID_VALUE: name");}
 @Test void permitsNormalJsonWhitespace(){var values=product("Description");values.put("skus","[\n {\"description\":\"Pack\",\"price\":1,\"quantity\":1}\n]");assertThat(ImportTool.PRODUCT_BULK_CREATE.validate(values)).isEmpty();}
}
