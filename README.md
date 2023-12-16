# Interception of the [Emission](https://github.com/MoodMinds/emission) and [Traverse Streams Traversable](https://github.com/MoodMinds/traverse-streams-traversable) return types when marked with [Spring](https://spring.io)'s @Transactional

`@Transactional` interceptor registration of the [Emission](https://github.com/MoodMinds/emission)'s `Emittable` and
[Traverse Streams Traversable](https://github.com/MoodMinds/traverse-streams-traversable)'s `Traversable` in Spring's
`BeanFactoryTransactionAttributeSourceAdvisor` in Servlet non-reactive context.

## Key Notes

If annotated with `@Transactional`, the traverse methods of an `Emittable` or `Traversable` undergo interception, incorporating
additional pre- and post-transaction demarcating executions. The traversal's possibly negative completion, indicated by a `boolean`
result, as well as thrown exceptions, serves as a signal to consider a transaction rollback.

## Usage

Include the provided `TraverseSupportTransactionAdvisory` in your Spring config and be able to use `Emittable` and `Traversable`
as return value in methods marked with `@Transactional` annotation:

```java
import org.moodminds.emission.Emittable;
import org.moodminds.traverse.Traversable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static org.moodminds.emission.Emittable.emittable;
import static org.moodminds.traverse.Traversable.traversable;

@Service
public class SampleService {

    @Transactional
    public Emittable<String, Exception> emission() {
        return emittable(traversable());
    }

    @Transactional
    public Traversable<String, Exception> traverse() {
        return traversable();
    }
}
```

The returning `Emittable` or `Traversable` will be intercepted and wrapped with theirs transactional equivalent.

## Maven configuration

Artifacts can be found on [Maven Central](https://search.maven.org/) after publication.

```xml

<dependency>
    <groupId>org.moodminds.emission</groupId>
    <artifactId>emission-spring-tx</artifactId>
    <version>${version}</version>
</dependency>
```

## Building from Source

You may need to build from source to use **Emission Spring Transactions** (until it is in Maven Central) with Maven and JDK 8 at least.

## License
This project is going to be released under version 2.0 of the [Apache License][l].

[l]: https://www.apache.org/licenses/LICENSE-2.0