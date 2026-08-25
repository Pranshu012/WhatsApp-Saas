package com.example.wasaas.arch;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RestController;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

public class ArchitectureBoundaryTest {

    private static final ArchRule whatsapp_cloud_client_only_accessed_by_send_package =
            classes().that().haveSimpleName("WhatsAppCloudClient")
                    .should().onlyBeAccessed().byClassesThat()
                    .resideInAnyPackage("..whatsapp.send..", "..whatsapp.client..", "..whatsapp");

    private static final ArchRule webhook_package_must_not_access_http_client =
            noClasses().that().resideInAPackage("..whatsapp.webhook..")
                    .should().dependOnClassesThat()
                    .resideInAnyPackage("..whatsapp.client..", "..java.net.http..");

    private static final ArchRule rest_controllers_must_not_be_annotated_with_transactional =
            noClasses().that().areAnnotatedWith(RestController.class)
                    .should().beAnnotatedWith(Transactional.class);

    @Test
    void verifyArchitectureRules() {
        var importedClasses = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("com.example.wasaas");

        whatsapp_cloud_client_only_accessed_by_send_package.check(importedClasses);
        webhook_package_must_not_access_http_client.check(importedClasses);
        rest_controllers_must_not_be_annotated_with_transactional.check(importedClasses);
    }
}
