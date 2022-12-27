package kg.musabaev.megalabnews.util.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.hibernate.validator.internal.constraintvalidators.hv.URLValidator;

import java.net.MalformedURLException;

/**
 * @see URLValidator
 */
public class UrlOrNullValidator implements ConstraintValidator<UrlOrNull, CharSequence> {

	private String protocol;
	private String host;
	private int port;

	@Override
	public void initialize(UrlOrNull url) {
		this.protocol = url.protocol();
		this.host = url.host();
		this.port = url.port();
	}

	@Override
	public boolean isValid(CharSequence value, ConstraintValidatorContext constraintValidatorContext) {
		if (value == null) return true;
		if (value.length() != 0) {
			java.net.URL url;
			try {
				url = new java.net.URL(value.toString());
			} catch (MalformedURLException var5) {
				return false;
			}
			if (this.protocol != null && this.protocol.length() > 0 && !url.getProtocol().equals(this.protocol))
				return false;
			else if (this.host != null && this.host.length() > 0 && !url.getHost().equals(this.host))
				return false;
			else
				return this.port == -1 || url.getPort() == this.port;
		} else
			return true;
	}
}
