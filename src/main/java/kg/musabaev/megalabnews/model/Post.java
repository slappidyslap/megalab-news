package kg.musabaev.megalabnews.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.springframework.lang.Nullable;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "posts", indexes = {
		@Index(name = "posts_title_idx", columnList = "title", unique = true)
})
@FieldDefaults(level = AccessLevel.PRIVATE)
@Getter
@Setter
@ToString(exclude = {"tags"})
@NoArgsConstructor
@EqualsAndHashCode(of = {"title", "createdDate"})
public class Post {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "post_id", nullable = false)
	Long id;

	@Column(nullable = false, unique = true)
	String title;

	@Column(nullable = false, length = 500)
	String description = "";

	@CreationTimestamp
	@Column(updatable = false)
	LocalDate createdDate;

	@ElementCollection(fetch = FetchType.LAZY)
	@CollectionTable(name = "posts_tags", joinColumns = @JoinColumn(name = "post_id"))
	@Column(name = "tag")
	@Fetch(FetchMode.SUBSELECT)
	Set<String> tags = new HashSet<>();

	@Lob
	@Column(nullable = false, columnDefinition = "text")
	String content;

	@Column(length = 2000)
	@Nullable
	String coverPath;
}
