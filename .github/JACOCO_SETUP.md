# Configuração JaCoCo para Coverage Reports

Para habilitar os relatórios de cobertura de código, adicione o plugin JaCoCo ao `pom.xml`:

## Adicionar ao pom.xml

### 1. Na seção `<properties>`:
```xml
<properties>
    <java.version>21</java.version>
    <jacoco.version>0.8.10</jacoco.version>
    <maven.compiler.source>21</maven.compiler.source>
    <maven.compiler.target>21</maven.compiler.target>
</properties>
```

### 2. Na seção `<build><plugins>`:
```xml
<build>
    <plugins>
        <!-- Plugin do Spring Boot já existente -->
        <plugin>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-maven-plugin</artifactId>
        </plugin>

        <!-- Adicionar plugin JaCoCo -->
        <plugin>
            <groupId>org.jacoco</groupId>
            <artifactId>jacoco-maven-plugin</artifactId>
            <version>${jacoco.version}</version>
            <executions>
                <execution>
                    <goals>
                        <goal>prepare-agent</goal>
                    </goals>
                </execution>
                <execution>
                    <id>report</id>
                    <phase>test</phase>
                    <goals>
                        <goal>report</goal>
                    </goals>
                </execution>
                <execution>
                    <id>check</id>
                    <goals>
                        <goal>check</goal>
                    </goals>
                    <configuration>
                        <rules>
                            <rule>
                                <element>BUNDLE</element>
                                <limits>
                                    <limit>
                                        <counter>INSTRUCTION</counter>
                                        <value>COVEREDRATIO</value>
                                        <minimum>0.70</minimum>
                                    </limit>
                                    <limit>
                                        <counter>BRANCH</counter>
                                        <value>COVEREDRATIO</value>
                                        <minimum>0.60</minimum>
                                    </limit>
                                </limits>
                            </rule>
                        </rules>
                    </configuration>
                </execution>
            </executions>
        </plugin>
    </plugins>
</build>
```

## Comandos para Coverage

### Gerar relatório de cobertura:
```bash
./mvnw clean test jacoco:report
```

### Verificar thresholds de cobertura:
```bash
./mvnw jacoco:check
```

### Visualizar relatório:
```bash
open target/site/jacoco/index.html
```

## Configuração no GitHub Actions

O workflow `code-quality.yml` já está configurado para:
- ✅ Executar testes com cobertura
- ✅ Gerar relatórios JaCoCo
- ✅ Verificar thresholds mínimos (70% overall, 80% arquivos alterados)
- ✅ Comentar no PR com resultados de cobertura
- ✅ Fazer upload dos relatórios como artefatos

## Thresholds Configurados

- **Cobertura geral mínima**: 70%
- **Cobertura de arquivos alterados**: 80%
- **Cobertura de branches**: 60%

## Exclusões Recomendadas

Para excluir classes de configuração e DTOs da cobertura, adicione:

```xml
<configuration>
    <excludes>
        <exclude>**/config/**</exclude>
        <exclude>**/dto/**</exclude>
        <exclude>**/entity/**</exclude>
        <exclude>**/Application.class</exclude>
    </excludes>
</configuration>
```

## Workflow Opcional

Se não quiser coverage obrigatório, você pode desabilitar o workflow `code-quality.yml` removendo o arquivo ou comentando as linhas de verificação de threshold.