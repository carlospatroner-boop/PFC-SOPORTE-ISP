package ec.edu.uteq.soporte.ticketservice.architecture;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Entregable 1 de la guia de cierre: "el dominio importa el marco". Antes de esta prueba,
 * domain/ tenia 12 lineas de import org.springframework.* en 9 clases (@Component/@Value/
 * @Order, para que Spring pudiera descubrirlas como beans) mientras el manuscrito publicaba
 * "cero anotaciones Spring/JPA" -- movidas a infrastructure/config/DomainBeansConfig.java.
 * Esta prueba falla la construccion si alguna clase de domain vuelve a depender de Spring o
 * de JPA, en vez de depender de que alguien lo note leyendo el codigo a mano.
 */
@AnalyzeClasses(packages = "ec.edu.uteq.soporte.ticketservice")
class DomainArchitectureTest {

    @ArchTest
    static final ArchRule domain_no_depende_de_spring_ni_de_jpa = noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "org.springframework..", "jakarta.persistence..", "javax.persistence..");
}
