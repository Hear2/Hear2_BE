package com.hear2.character.dto;

public record CharacterExpGrantResult(
        boolean granted,
        long requestedExp,
        long grantedExp,
        boolean duplicated,
        boolean limited
) {

    public static CharacterExpGrantResult duplicated(long requestedExp) {
        return new CharacterExpGrantResult(false, requestedExp, 0L, true, false);
    }

    public static CharacterExpGrantResult limitedOut(long requestedExp) {
        return new CharacterExpGrantResult(false, requestedExp, 0L, false, true);
    }

    public static CharacterExpGrantResult granted(long requestedExp, long grantedExp) {
        return new CharacterExpGrantResult(true, requestedExp, grantedExp, false, grantedExp < requestedExp);
    }
}
