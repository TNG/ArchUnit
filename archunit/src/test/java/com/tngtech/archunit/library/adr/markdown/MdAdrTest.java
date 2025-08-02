package com.tngtech.archunit.library.adr.markdown;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

class MdAdrTest {
    @Test
    void testMdGeneration() {
        MatcherAssert.assertThat(
                new MdAdr(
                        "{short title, representative of solved problem and found solution}",
                        "{Describe the context and problem statement, e.g., in free form using two to three sentences or in the form of an illustrative story. You may want to articulate the problem in form of a question and add links to collaboration boards or issue management systems.}",
                        Arrays.asList(
                                "{title of option 1}",
                                "{title of option 2}",
                                "{title of option 3}"
                        ),
                        "Chosen option: \"{title of option 1}\", because {justification. e.g., only option, which meets k.o. criterion decision driver | which resolves force {force} | ... | comes out best (see below)}."
                ).withMetadata(
                        new MdMetadata().withStatus(
                                "accepted"
                        ).withDate(
                                "YYYY-MM-DD"
                        ).withDecisionMakers(
                                Arrays.asList(
                                        "John Doe",
                                        "Romain Rochegude"
                                )
                        ).withConsulted(
                                Arrays.asList(
                                        "John Doe",
                                        "Romain Rochegude"
                                )
                        ).withInformed(
                                Arrays.asList(
                                        "John Doe",
                                        "Romain Rochegude"
                                )
                        )
                ).withDecisionDrivers(
                        Arrays.asList(
                                "{decision driver 1, e.g., a force, facing concern, ...}",
                                "{decision driver 2, e.g., a force, facing concern, ...}"
                        )
                ).withConsequences(
                        Arrays.asList(
                                "Good, because {positive consequence, e.g., improvement of one or more desired qualities, ...}",
                                "Bad, because {negative consequence, e.g., compromising one or more desired qualities, ...}"
                        )
                ).withConfirmation(
                        "{Describe how the implementation / compliance of the ADR can/will be confirmed. Is there any automated or manual fitness function? If so, list it and explain how it is applied. Is the chosen design and its implementation in line with the decision? E.g., a design/code review or a test with a library such as ArchUnit can help validate this. Note that although we classify this element as optional, it is included in many ADRs.}"
                ).withOptionProsAndCons(
                        Arrays.asList(
                                new MdOptionProsAndCons(
                                        "{title of option 1}",
                                        Arrays.asList(
                                                "Good, because {argument a}",
                                                "Good, because {argument b}",
                                                "Neutral, because {argument c}",
                                                "Bad, because {argument d}"
                                        )
                                ).withDescription("{description}").withExample("{example}"),
                                new MdOptionProsAndCons(
                                        "{title of other option}",
                                        Arrays.asList(
                                                "Good, because {argument a}",
                                                "Good, because {argument b}",
                                                "Neutral, because {argument c}",
                                                "Bad, because {argument d}"
                                        )
                                ).withDescription("{description}").withExample("{example}")
                        )
                ).withMoreInformation(
                        "{You might want to provide additional evidence/confidence for the decision outcome here and/or document the team agreement on the decision and/or define when/how this decision the decision should be realized and if/when it should be re-visited. Links to other decisions and resources might appear here as well.}"
                ).toString(),
                new IsEqual<>(
                        "---\n" +
                                "status: accepted\n" +
                                "date: YYYY-MM-DD\n" +
                                "decision-makers: John Doe, Romain Rochegude\n" +
                                "consulted: John Doe, Romain Rochegude\n" +
                                "informed: John Doe, Romain Rochegude\n" +
                                "---\n" +
                                "\n" +
                                "# {short title, representative of solved problem and found solution}\n" +
                                "\n" +
                                "## Context and Problem Statement\n" +
                                "\n" +
                                "{Describe the context and problem statement, e.g., in free form using two to three sentences or in the form of an illustrative story. You may want to articulate the problem in form of a question and add links to collaboration boards or issue management systems.}\n" +
                                "\n" +
                                "## Decision Drivers\n" +
                                "\n" +
                                "* {decision driver 1, e.g., a force, facing concern, ...}\n" +
                                "* {decision driver 2, e.g., a force, facing concern, ...}\n" +
                                "\n" +
                                "## Considered Options\n" +
                                "\n" +
                                "* {title of option 1}\n" +
                                "* {title of option 2}\n" +
                                "* {title of option 3}\n" +
                                "\n" +
                                "## Decision Outcome\n" +
                                "\n" +
                                "Chosen option: \"{title of option 1}\", because {justification. e.g., only option, which meets k.o. criterion decision driver | which resolves force {force} | ... | comes out best (see below)}.\n" +
                                "\n" +
                                "### Consequences\n" +
                                "\n" +
                                "* Good, because {positive consequence, e.g., improvement of one or more desired qualities, ...}\n" +
                                "* Bad, because {negative consequence, e.g., compromising one or more desired qualities, ...}\n" +
                                "\n" +
                                "### Confirmation\n" +
                                "\n" +
                                "{Describe how the implementation / compliance of the ADR can/will be confirmed. Is there any automated or manual fitness function? If so, list it and explain how it is applied. Is the chosen design and its implementation in line with the decision? E.g., a design/code review or a test with a library such as ArchUnit can help validate this. Note that although we classify this element as optional, it is included in many ADRs.}\n" +
                                "\n" +
                                "## Pros and Cons of the Options\n" +
                                "\n" +
                                "### {title of option 1}\n" +
                                "\n" +
                                "{description}\n" +
                                "\n" +
                                "{example}\n" +
                                "\n" +
                                "* Good, because {argument a}\n" +
                                "* Good, because {argument b}\n" +
                                "* Neutral, because {argument c}\n" +
                                "* Bad, because {argument d}\n" +
                                "\n" +
                                "### {title of other option}\n" +
                                "\n" +
                                "{description}\n" +
                                "\n" +
                                "{example}\n" +
                                "\n" +
                                "* Good, because {argument a}\n" +
                                "* Good, because {argument b}\n" +
                                "* Neutral, because {argument c}\n" +
                                "* Bad, because {argument d}\n" +
                                "\n" +
                                "## More Information\n" +
                                "\n" +
                                "{You might want to provide additional evidence/confidence for the decision outcome here and/or document the team agreement on the decision and/or define when/how this decision the decision should be realized and if/when it should be re-visited. Links to other decisions and resources might appear here as well.}"
                )
        );
    }
}
