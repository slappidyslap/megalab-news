package kg.musabaev.megalabnews.security;

public enum Authority {
	READ_POST, WRITE_POST,
	READ_COMMENT, WRITE_COMMENT,
	READ_USER, WRITE_USER,

	READ_H2_CONSOLE,
	READ_ACTUATOR,
}
