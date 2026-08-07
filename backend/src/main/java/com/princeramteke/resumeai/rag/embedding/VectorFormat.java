package com.princeramteke.resumeai.rag.embedding;

/**
 * Formats an embedding vector as a pgvector literal (e.g. {@code [0.1,0.2,0.3]}) for use as
 * a bound, parameterized value cast with {@code CAST(? AS vector)} in native queries.
 */
public final class VectorFormat {

    private VectorFormat() {
    }

    public static String toSqlString(float[] vector) {
        if (vector == null || vector.length == 0) {
            throw new IllegalArgumentException("Embedding vector must not be empty");
        }
        StringBuilder sb = new StringBuilder(vector.length * 8);
        sb.append('[');
        for (int i = 0; i < vector.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(vector[i]);
        }
        sb.append(']');
        return sb.toString();
    }
}
