package by.Egrius.currency_service.entity;

import lombok.Getter;

@Getter
public enum Trend {

    UPWARD("UPWARD"), DOWNWARD("DOWNWARD"), FLAT("FLAT");

    private final String abbr;

    Trend(String abbr) {
        this.abbr = abbr;
    }

    public String getAbbr() {
        return abbr;
    }
}
