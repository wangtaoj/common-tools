package com.wangtao.tools.jackson;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;
import java.math.BigDecimal;

/**
 * BigDecimal序列化器
 *
 * @author wangtao
 * Created at 2023-09-26
 */
public class BigDecimalSerializer extends StdSerializer<BigDecimal> {

    private static final long serialVersionUID = -82683766308966384L;

    public BigDecimalSerializer() {
        super(BigDecimal.class);
    }

    @Override
    public void serialize(BigDecimal value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        gen.writeString(value.stripTrailingZeros().toPlainString());
    }
}
