# Generador-Pseudoaleatorio-con-Maquina-de-Turing
## Objetivo
Implementar una Máquina de Turing que simule el comportamiento de un generador de
números pseudoaleatorios del tipo XOR-Shift. El propósito del trabajo es comprender cómo
una máquina determinista puede generar secuencias que parecieran ser aleatorias, y
estudiar cómo el período (repetición, hasta volver a encontrarse el número disparador)
depende de los parámetros y del tamaño de palabra binaria.
## Requisitos

- **Java 21** o superior
- **Maven 3.6** o superior

### Instalación de Java y Maven

#### En Linux (Debian/Ubuntu)

```sh
sudo apt update
sudo apt install openjdk-21-jdk maven
```
### En Windows
- Descargar e instalar [java 21 JDK](https://www.oracle.com/java/technologies/downloads/).
- Descargar e instalar [Maven](https://maven.apache.org/download.cgi).

Para verificar la instalación, ejecute en la terminal o consola:
```sh
java -version
mvn -version
```
### Compilación y ejecución
Desde la raíz del proyecto, ejecutar:
```sh
mvn clean compile o mvn clean packege
```
Esto genera un archivo ```.jar``` en la carpeta ```target/```.</br>
Para ejecutar el programa:
```sh
mvn exc:java
```
o directamente
```sh
java -cp target/automatas-1.0-SNAPSHOT.jar com.unpsjb.machineturing.application.Main
```

### Dependencias
El proyecto utiliza las siguientes librería:
- [iText 7 v 7.2.5](https://itextpdf.com/) para generar PDFs.
Estas dependencias se gestionan automáticamente con **Maven**.
</br>
Nota: Si usan VScode deben tener instalado ```Extension Pack for Java```.
