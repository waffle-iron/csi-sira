package org.geoserver.security.iride.util.validator;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.geoserver.security.iride.util.Flag;

/**
 *
 * @author "Simone Cornacchia - seancrow76@gmail.com, simone.cornacchia@consulenti.csi.it (CSI:71740)"
 */
public final class IdentitaIrideValidator {

    public static final long CODICE_FISCALE_WEAK_VALIDATION = 1 << 0;

    public static final long CODICE_FISCALE_STRONG_VALIDATION = 1 << 1;

    public static final long TIMESTAMP_NUMBERS_VALIDATION = 1 << 2;

    /**
     * Singleton instance.
     */
    private static final IdentitaIrideValidator DEFAULT_IDENTITA_IRIDE_VALIDATOR = new IdentitaIrideValidator();

    private static final char IDENTITA_IRIDE_TOKEN_SEPARATOR = '/';

    private static final Pattern CODICE_FISCALE_WEAK_VALIDATION_PATTERN = Pattern.compile("^[A-Z]{6}\\d{2}[A-Z]\\d{2}[A-Z]\\d{3}[A-Z]$");

    private static final Pattern CODICE_FISCALE_STRONG_VALIDATION_PATTERN = Pattern.compile("^(?:[B-DF-HJ-NP-TV-Z](?:[AEIOU]{2}|[AEIOU]X)|[AEIOU]{2}X|[B-DF-HJ-NP-TV-Z]{2}[A-Z]){2}[\\dLMNP-V]{2}(?:[A-EHLMPR-T](?:[04LQ][1-9MNP-V]|[1256LMRS][\\dLMNP-V])|[DHPS][37PT][0L]|[ACELMRT][37PT][01LM])(?:[A-MZ][1-9MNP-V][\\dLMNP-V]{2}|[A-M][0L](?:[\\dLMNP-V][1-9MNP-V]|[1-9MNP-V][0L]))[A-Z]$");

    private static final Pattern TIMESTAMP_NUMBERS_VALIDATION_PATTERN = Pattern.compile("^\\d{4}\\d{2}\\d{2}\\d{2}\\d{2}\\d{2}$");

    /**
     * Returns the singleton instance.
     *
     * @return singleton instance
     */
    public static IdentitaIrideValidator getInstance() {
        return DEFAULT_IDENTITA_IRIDE_VALIDATOR;
    }

    /**
     * Holds the set of current validation options.
     */
    private final long options;

    /**
     * {@link DateFormat} instance.
     */
    private final DateFormat dateFormat;

    /**
     * Constructor.
     */
    public IdentitaIrideValidator() {
        this(CODICE_FISCALE_WEAK_VALIDATION + TIMESTAMP_NUMBERS_VALIDATION);
    }

    /**
     * Constructor.
     *
     * @param options
     */
    public IdentitaIrideValidator(long options) {
        if (options < 0) {
            throw new IllegalArgumentException("Negative options not allowed!");
        }

        this.options    = options;
        this.dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
    }

    /**
     *
     * @param value
     * @return
     */
    public boolean isValid(String value) {
        if (value == null) {
            return false;
        }

        // Check the number of tokens
        final String[] tokens = StringUtils.split(value, IDENTITA_IRIDE_TOKEN_SEPARATOR);
        if (tokens.length != 7) {
            return false;
        }

        // Check "codice fiscale" token
        if (! this.isValidCodiceFiscale(tokens[0])) {
            return false;
        }

        // Check "nome" token
        if (! this.isValidNome(tokens[1])) {
            return false;
        }

        // Check "cognome" token
        if (! this.isValidCognome(tokens[2])) {
            return false;
        }

        // Check "idProvider" token
        if (! this.isValidIdProvider(tokens[3])) {
            return false;
        }

        // Check "timestamp" token
        if (! this.isValidTimestamp(tokens[4])) {
            return false;
        }

        // Check "livelloAutenticazione" token
        if (! this.isValidLivelloAutenticazione(tokens[5])) {
            return false;
        }

        // Check "mac" token
        if (! this.isValidMac(tokens[6])) {
            return false;
        }

        return true;
    }



    /**
     * Validates <code>Codice Fiscale</code> token.
     *
     * @param value
     * @return
     */
    private boolean isValidCodiceFiscale(String value) {
        if (StringUtils.isBlank(value)) {
            return false;
        }

        if (Flag.isOn(this.options, CODICE_FISCALE_WEAK_VALIDATION) &&
            ! CODICE_FISCALE_WEAK_VALIDATION_PATTERN.matcher(value).matches()) {
            return false;
        }

        if (Flag.isOn(this.options, CODICE_FISCALE_STRONG_VALIDATION) &&
            ! CODICE_FISCALE_STRONG_VALIDATION_PATTERN.matcher(value).matches()) {
            return false;
        }

        return true;
    }

    /**
     * Validates <code>Nome</code> token.
     *
     * @param value
     * @return
     */
    private boolean isValidNome(String value) {
        return StringUtils.isNotBlank(value);
    }

    /**
     * Validates <code>Cognome</code> token.
     *
     * @param value
     * @return
     */
    private boolean isValidCognome(String value) {
        return StringUtils.isNotBlank(value);
    }

    /**
     * Validates <code>IdProvider</code> token.
     *
     * @param value
     * @return
     */
    private boolean isValidIdProvider(String value) {
        return StringUtils.isNotBlank(value);
    }

    /**
     * Validates <code>Timestamp</code> token.
     *
     * @param value
     * @return
     */
    private boolean isValidTimestamp(String value) {
        if (StringUtils.isBlank(value)) {
            return false;
        }

        try {
            this.dateFormat.parse(value);

            if (Flag.isOn(this.options, TIMESTAMP_NUMBERS_VALIDATION) &&
                ! TIMESTAMP_NUMBERS_VALIDATION_PATTERN.matcher(value).matches()) {
                return false;
            }

            return true;
        } catch (ParseException e) {
            return false;
        }
    }

    /**
     * Validates <code>livelloAutenticazione</code> token.
     *
     * @param value
     * @return
     */
    private boolean isValidLivelloAutenticazione(String value) {
        if (StringUtils.isBlank(value)) {
            return false;
        }

        try {
            final int livello = Integer.valueOf(value);
            if (livello < 1 && livello > 3) {
                return false;
            }

            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Validates <code>mac</code> token.
     *
     * @param value
     * @return
     */
    private boolean isValidMac(String value) {
        return StringUtils.isNotBlank(value);
    }

}