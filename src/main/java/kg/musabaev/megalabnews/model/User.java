package kg.musabaev.megalabnews.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import kg.musabaev.megalabnews.security.Authority;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


@Entity
@Table(name = "users", indexes = {
		@Index(name = "users_username_idx", columnList = "username", unique = true)
})
@FieldDefaults(level = AccessLevel.PRIVATE)
@Getter
@Setter
@ToString(exclude = "favouritePosts")
@EqualsAndHashCode(of = "username")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "user_id", nullable = false)
	Long id;

	String name;

	String surname;

	@Column(nullable = false, unique = true)
	String username;

	@Column(nullable = false)
	@JsonIgnore
	String password;

	@ElementCollection(fetch = FetchType.EAGER)
	@CollectionTable(
			name = "users_authorities",
			joinColumns = @JoinColumn(name = "user_id", nullable = false))
	@Column(name = "authority", nullable = false)
	@Enumerated(EnumType.STRING)
	@Fetch(FetchMode.SUBSELECT)
	@Singular
	Set<Authority> authorities = new HashSet<>();

	@ManyToMany(
			fetch = FetchType.LAZY,
			cascade = {CascadeType.PERSIST, CascadeType.MERGE})
	@JoinTable(
			name = "favourite_posts_users",
			joinColumns = @JoinColumn(name = "user_id", nullable = false),
			inverseJoinColumns = @JoinColumn(name = "post_id", nullable = false))
	@Fetch(FetchMode.SUBSELECT)
	List<Post> favouritePosts = new ArrayList<>();

	@OneToMany(
			fetch = FetchType.LAZY,
			cascade = CascadeType.ALL,
			mappedBy = "id",
			orphanRemoval = true)
	List<Post> createdPosts = new ArrayList<>();

	@Column(length = 2000)
	@Builder.Default
	String userPictureUrl = "https://i.pinimg.com/474x/20/0d/72/200d72a18492cf3d7adac8a914ef3520.jpg";
}
