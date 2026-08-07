package com.princeramteke.resumeai.rag.embedding;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class VectorFormatTest {

    @Test
    void toSqlString_formatsAsPgvectorLiteral() {
        String literal = VectorFormat.toSqlString(new float[]{0.1f, 0.2f, 0.3f});

        assertThat(literal).startsWith("[").endsWith("]");
        assertThat(literal).isEqualTo("[0.1,0.2,0.3]");
    }

    @Test
    void toSqlString_singleElement() {
        assertThat(VectorFormat.toSqlString(new float[]{1.5f})).isEqualTo("[1.5]");
    }

    @Test
    void toSqlString_nullVector_throws() {
        assertThatThrownBy(() -> VectorFormat.toSqlString(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void toSqlString_emptyVector_throws() {
        assertThatThrownBy(() -> VectorFormat.toSqlString(new float[]{}))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
