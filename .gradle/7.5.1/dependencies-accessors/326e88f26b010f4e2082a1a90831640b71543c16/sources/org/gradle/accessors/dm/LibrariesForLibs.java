package org.gradle.accessors.dm;

import org.gradle.api.NonNullApi;
import org.gradle.api.artifacts.MinimalExternalModuleDependency;
import org.gradle.plugin.use.PluginDependency;
import org.gradle.api.artifacts.ExternalModuleDependencyBundle;
import org.gradle.api.artifacts.MutableVersionConstraint;
import org.gradle.api.provider.Provider;
import org.gradle.api.provider.ProviderFactory;
import org.gradle.api.internal.catalog.AbstractExternalDependencyFactory;
import org.gradle.api.internal.catalog.DefaultVersionCatalog;
import java.util.Map;
import javax.inject.Inject;

/**
 * A catalog of dependencies accessible via the `libs` extension.
*/
@NonNullApi
public class LibrariesForLibs extends AbstractExternalDependencyFactory {

    private final AbstractExternalDependencyFactory owner = this;
    private final BioimageioLibraryAccessors laccForBioimageioLibraryAccessors = new BioimageioLibraryAccessors(owner);
    private final CommonsLibraryAccessors laccForCommonsLibraryAccessors = new CommonsLibraryAccessors(owner);
    private final CudaLibraryAccessors laccForCudaLibraryAccessors = new CudaLibraryAccessors(owner);
    private final GroovyLibraryAccessors laccForGroovyLibraryAccessors = new GroovyLibraryAccessors(owner);
    private final IkonliLibraryAccessors laccForIkonliLibraryAccessors = new IkonliLibraryAccessors(owner);
    private final JunitLibraryAccessors laccForJunitLibraryAccessors = new JunitLibraryAccessors(owner);
    private final OpencvLibraryAccessors laccForOpencvLibraryAccessors = new OpencvLibraryAccessors(owner);
    private final VersionAccessors vaccForVersionAccessors = new VersionAccessors(providers, config);
    private final BundleAccessors baccForBundleAccessors = new BundleAccessors(providers, config);
    private final PluginAccessors paccForPluginAccessors = new PluginAccessors(providers, config);

    @Inject
    public LibrariesForLibs(DefaultVersionCatalog config, ProviderFactory providers) {
        super(config, providers);
    }

        /**
         * Creates a dependency provider for commonmark (org.commonmark:commonmark)
         * This dependency was declared in catalog io.github.qupath:qupath-catalog:0.4.4
         */
        public Provider<MinimalExternalModuleDependency> getCommonmark() { return create("commonmark"); }

        /**
         * Creates a dependency provider for controlsfx (org.controlsfx:controlsfx)
         * This dependency was declared in catalog io.github.qupath:qupath-catalog:0.4.4
         */
        public Provider<MinimalExternalModuleDependency> getControlsfx() { return create("controlsfx"); }

        /**
         * Creates a dependency provider for gson (com.google.code.gson:gson)
         * This dependency was declared in catalog io.github.qupath:qupath-catalog:0.4.4
         */
        public Provider<MinimalExternalModuleDependency> getGson() { return create("gson"); }

        /**
         * Creates a dependency provider for guava (com.google.guava:guava)
         * This dependency was declared in catalog io.github.qupath:qupath-catalog:0.4.4
         */
        public Provider<MinimalExternalModuleDependency> getGuava() { return create("guava"); }

        /**
         * Creates a dependency provider for imagej (net.imagej:ij)
         * This dependency was declared in catalog io.github.qupath:qupath-catalog:0.4.4
         */
        public Provider<MinimalExternalModuleDependency> getImagej() { return create("imagej"); }

        /**
         * Creates a dependency provider for javacpp (org.bytedeco:javacpp)
         * This dependency was declared in catalog io.github.qupath:qupath-catalog:0.4.4
         */
        public Provider<MinimalExternalModuleDependency> getJavacpp() { return create("javacpp"); }

        /**
         * Creates a dependency provider for jfreesvg (org.jfree:org.jfree.svg)
         * This dependency was declared in catalog io.github.qupath:qupath-catalog:0.4.4
         */
        public Provider<MinimalExternalModuleDependency> getJfreesvg() { return create("jfreesvg"); }

        /**
         * Creates a dependency provider for jfxtras (org.jfxtras:jfxtras-menu)
         * This dependency was declared in catalog io.github.qupath:qupath-catalog:0.4.4
         */
        public Provider<MinimalExternalModuleDependency> getJfxtras() { return create("jfxtras"); }

        /**
         * Creates a dependency provider for jts (org.locationtech.jts:jts-core)
         * This dependency was declared in catalog io.github.qupath:qupath-catalog:0.4.4
         */
        public Provider<MinimalExternalModuleDependency> getJts() { return create("jts"); }

        /**
         * Creates a dependency provider for logback (ch.qos.logback:logback-classic)
         * This dependency was declared in catalog io.github.qupath:qupath-catalog:0.4.4
         */
        public Provider<MinimalExternalModuleDependency> getLogback() { return create("logback"); }

        /**
         * Creates a dependency provider for picocli (info.picocli:picocli)
         * This dependency was declared in catalog io.github.qupath:qupath-catalog:0.4.4
         */
        public Provider<MinimalExternalModuleDependency> getPicocli() { return create("picocli"); }

        /**
         * Creates a dependency provider for richtextfx (org.fxmisc.richtext:richtextfx)
         * This dependency was declared in catalog io.github.qupath:qupath-catalog:0.4.4
         */
        public Provider<MinimalExternalModuleDependency> getRichtextfx() { return create("richtextfx"); }

        /**
         * Creates a dependency provider for slf4j (org.slf4j:slf4j-api)
         * This dependency was declared in catalog io.github.qupath:qupath-catalog:0.4.4
         */
        public Provider<MinimalExternalModuleDependency> getSlf4j() { return create("slf4j"); }

        /**
         * Creates a dependency provider for snakeyaml (org.yaml:snakeyaml)
         * This dependency was declared in catalog io.github.qupath:qupath-catalog:0.4.4
         */
        public Provider<MinimalExternalModuleDependency> getSnakeyaml() { return create("snakeyaml"); }

    /**
     * Returns the group of libraries at bioimageio
     */
    public BioimageioLibraryAccessors getBioimageio() { return laccForBioimageioLibraryAccessors; }

    /**
     * Returns the group of libraries at commons
     */
    public CommonsLibraryAccessors getCommons() { return laccForCommonsLibraryAccessors; }

    /**
     * Returns the group of libraries at cuda
     */
    public CudaLibraryAccessors getCuda() { return laccForCudaLibraryAccessors; }

    /**
     * Returns the group of libraries at groovy
     */
    public GroovyLibraryAccessors getGroovy() { return laccForGroovyLibraryAccessors; }

    /**
     * Returns the group of libraries at ikonli
     */
    public IkonliLibraryAccessors getIkonli() { return laccForIkonliLibraryAccessors; }

    /**
     * Returns the group of libraries at junit
     */
    public JunitLibraryAccessors getJunit() { return laccForJunitLibraryAccessors; }

    /**
     * Returns the group of libraries at opencv
     */
    public OpencvLibraryAccessors getOpencv() { return laccForOpencvLibraryAccessors; }

    /**
     * Returns the group of versions at versions
     */
    public VersionAccessors getVersions() { return vaccForVersionAccessors; }

    /**
     * Returns the group of bundles at bundles
     */
    public BundleAccessors getBundles() { return baccForBundleAccessors; }

    /**
     * Returns the group of plugins at plugins
     */
    public PluginAccessors getPlugins() { return paccForPluginAccessors; }

    public static class BioimageioLibraryAccessors extends SubDependencyFactory {

        public BioimageioLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

            /**
             * Creates a dependency provider for spec (io.github.qupath:qupath-bioimageio-spec)
             * This dependency was declared in catalog io.github.qupath:qupath-catalog:0.4.4
             */
            public Provider<MinimalExternalModuleDependency> getSpec() { return create("bioimageio.spec"); }

    }

    public static class CommonsLibraryAccessors extends SubDependencyFactory {

        public CommonsLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

            /**
             * Creates a dependency provider for math (org.apache.commons:commons-math3)
             * This dependency was declared in catalog io.github.qupath:qupath-catalog:0.4.4
             */
            public Provider<MinimalExternalModuleDependency> getMath() { return create("commons.math"); }

            /**
             * Creates a dependency provider for text (org.apache.commons:commons-text)
             * This dependency was declared in catalog io.github.qupath:qupath-catalog:0.4.4
             */
            public Provider<MinimalExternalModuleDependency> getText() { return create("commons.text"); }

    }

    public static class CudaLibraryAccessors extends SubDependencyFactory implements DependencyNotationSupplier {

        public CudaLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

            /**
             * Creates a dependency provider for cuda (org.bytedeco:cuda-platform)
             * This dependency was declared in catalog io.github.qupath:qupath-catalog:0.4.4
             */
            public Provider<MinimalExternalModuleDependency> asProvider() { return create("cuda"); }

            /**
             * Creates a dependency provider for redist (org.bytedeco:cuda-platform-redist)
             * This dependency was declared in catalog io.github.qupath:qupath-catalog:0.4.4
             */
            public Provider<MinimalExternalModuleDependency> getRedist() { return create("cuda.redist"); }

    }

    public static class GroovyLibraryAccessors extends SubDependencyFactory {

        public GroovyLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

            /**
             * Creates a dependency provider for core (org.apache.groovy:groovy)
             * This dependency was declared in catalog io.github.qupath:qupath-catalog:0.4.4
             */
            public Provider<MinimalExternalModuleDependency> getCore() { return create("groovy.core"); }

            /**
             * Creates a dependency provider for jsr223 (org.apache.groovy:groovy-jsr223)
             * This dependency was declared in catalog io.github.qupath:qupath-catalog:0.4.4
             */
            public Provider<MinimalExternalModuleDependency> getJsr223() { return create("groovy.jsr223"); }

            /**
             * Creates a dependency provider for xml (org.apache.groovy:groovy-xml)
             * This dependency was declared in catalog io.github.qupath:qupath-catalog:0.4.4
             */
            public Provider<MinimalExternalModuleDependency> getXml() { return create("groovy.xml"); }

    }

    public static class IkonliLibraryAccessors extends SubDependencyFactory {

        public IkonliLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

            /**
             * Creates a dependency provider for ionicons4 (org.kordamp.ikonli:ikonli-ionicons4-pack)
             * This dependency was declared in catalog io.github.qupath:qupath-catalog:0.4.4
             */
            public Provider<MinimalExternalModuleDependency> getIonicons4() { return create("ikonli.ionicons4"); }

            /**
             * Creates a dependency provider for javafx (org.kordamp.ikonli:ikonli-javafx)
             * This dependency was declared in catalog io.github.qupath:qupath-catalog:0.4.4
             */
            public Provider<MinimalExternalModuleDependency> getJavafx() { return create("ikonli.javafx"); }

    }

    public static class JunitLibraryAccessors extends SubDependencyFactory implements DependencyNotationSupplier {

        public JunitLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

            /**
             * Creates a dependency provider for junit (org.junit.jupiter:junit-jupiter)
             * This dependency was declared in catalog io.github.qupath:qupath-catalog:0.4.4
             */
            public Provider<MinimalExternalModuleDependency> asProvider() { return create("junit"); }

            /**
             * Creates a dependency provider for api (org.junit.jupiter:junit-jupiter-api)
             * This dependency was declared in catalog io.github.qupath:qupath-catalog:0.4.4
             */
            public Provider<MinimalExternalModuleDependency> getApi() { return create("junit.api"); }

            /**
             * Creates a dependency provider for engine (org.junit.jupiter:junit-jupiter-engine)
             * This dependency was declared in catalog io.github.qupath:qupath-catalog:0.4.4
             */
            public Provider<MinimalExternalModuleDependency> getEngine() { return create("junit.engine"); }

    }

    public static class OpencvLibraryAccessors extends SubDependencyFactory implements DependencyNotationSupplier {

        public OpencvLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

            /**
             * Creates a dependency provider for opencv (org.bytedeco:opencv-platform)
             * This dependency was declared in catalog io.github.qupath:qupath-catalog:0.4.4
             */
            public Provider<MinimalExternalModuleDependency> asProvider() { return create("opencv"); }

            /**
             * Creates a dependency provider for gpu (org.bytedeco:opencv-platform-gpu)
             * This dependency was declared in catalog io.github.qupath:qupath-catalog:0.4.4
             */
            public Provider<MinimalExternalModuleDependency> getGpu() { return create("opencv.gpu"); }

    }

    public static class VersionAccessors extends VersionFactory  {

        public VersionAccessors(ProviderFactory providers, DefaultVersionCatalog config) { super(providers, config); }

            /**
             * Returns the version associated to this alias: bioformats (6.12.0)
             * If the version is a rich version and that its not expressible as a
             * single version string, then an empty string is returned.
             * This version was declared in catalog io.github.qupath:qupath-catalog:0.4.4
             */
            public Provider<String> getBioformats() { return getVersion("bioformats"); }

            /**
             * Returns the version associated to this alias: cuda (11.8-8.6-1.5.8)
             * If the version is a rich version and that its not expressible as a
             * single version string, then an empty string is returned.
             * This version was declared in catalog io.github.qupath:qupath-catalog:0.4.4
             */
            public Provider<String> getCuda() { return getVersion("cuda"); }

            /**
             * Returns the version associated to this alias: deepJavaLibrary (0.20.0)
             * If the version is a rich version and that its not expressible as a
             * single version string, then an empty string is returned.
             * This version was declared in catalog io.github.qupath:qupath-catalog:0.4.4
             */
            public Provider<String> getDeepJavaLibrary() { return getVersion("deepJavaLibrary"); }

            /**
             * Returns the version associated to this alias: groovy (4.0.9)
             * If the version is a rich version and that its not expressible as a
             * single version string, then an empty string is returned.
             * This version was declared in catalog io.github.qupath:qupath-catalog:0.4.4
             */
            public Provider<String> getGroovy() { return getVersion("groovy"); }

            /**
             * Returns the version associated to this alias: ikonli (12.3.1)
             * If the version is a rich version and that its not expressible as a
             * single version string, then an empty string is returned.
             * This version was declared in catalog io.github.qupath:qupath-catalog:0.4.4
             */
            public Provider<String> getIkonli() { return getVersion("ikonli"); }

            /**
             * Returns the version associated to this alias: javacpp (1.5.8)
             * If the version is a rich version and that its not expressible as a
             * single version string, then an empty string is returned.
             * This version was declared in catalog io.github.qupath:qupath-catalog:0.4.4
             */
            public Provider<String> getJavacpp() { return getVersion("javacpp"); }

            /**
             * Returns the version associated to this alias: javafx (19)
             * If the version is a rich version and that its not expressible as a
             * single version string, then an empty string is returned.
             * This version was declared in catalog io.github.qupath:qupath-catalog:0.4.4
             */
            public Provider<String> getJavafx() { return getVersion("javafx"); }

            /**
             * Returns the version associated to this alias: junit (5.9.1)
             * If the version is a rich version and that its not expressible as a
             * single version string, then an empty string is returned.
             * This version was declared in catalog io.github.qupath:qupath-catalog:0.4.4
             */
            public Provider<String> getJunit() { return getVersion("junit"); }

            /**
             * Returns the version associated to this alias: opencv (4.6.0-1.5.8)
             * If the version is a rich version and that its not expressible as a
             * single version string, then an empty string is returned.
             * This version was declared in catalog io.github.qupath:qupath-catalog:0.4.4
             */
            public Provider<String> getOpencv() { return getVersion("opencv"); }

    }

    public static class BundleAccessors extends BundleFactory {
        private final OpencvBundleAccessors baccForOpencvBundleAccessors = new OpencvBundleAccessors(providers, config);

        public BundleAccessors(ProviderFactory providers, DefaultVersionCatalog config) { super(providers, config); }

            /**
             * Creates a dependency bundle provider for groovy which is an aggregate for the following dependencies:
             * <ul>
             *    <li>org.apache.groovy:groovy</li>
             *    <li>org.apache.groovy:groovy-jsr223</li>
             *    <li>org.apache.groovy:groovy-xml</li>
             * </ul>
             * This bundle was declared in catalog io.github.qupath:qupath-catalog:0.4.4
             */
            public Provider<ExternalModuleDependencyBundle> getGroovy() { return createBundle("groovy"); }

            /**
             * Creates a dependency bundle provider for ikonli which is an aggregate for the following dependencies:
             * <ul>
             *    <li>org.kordamp.ikonli:ikonli-javafx</li>
             *    <li>org.kordamp.ikonli:ikonli-ionicons4-pack</li>
             * </ul>
             * This bundle was declared in catalog io.github.qupath:qupath-catalog:0.4.4
             */
            public Provider<ExternalModuleDependencyBundle> getIkonli() { return createBundle("ikonli"); }

            /**
             * Creates a dependency bundle provider for logging which is an aggregate for the following dependencies:
             * <ul>
             *    <li>org.slf4j:slf4j-api</li>
             *    <li>ch.qos.logback:logback-classic</li>
             * </ul>
             * This bundle was declared in catalog io.github.qupath:qupath-catalog:0.4.4
             */
            public Provider<ExternalModuleDependencyBundle> getLogging() { return createBundle("logging"); }

            /**
             * Creates a dependency bundle provider for markdown which is an aggregate for the following dependencies:
             * <ul>
             *    <li>org.commonmark:commonmark</li>
             * </ul>
             * This bundle was declared in catalog io.github.qupath:qupath-catalog:0.4.4
             */
            public Provider<ExternalModuleDependencyBundle> getMarkdown() { return createBundle("markdown"); }

            /**
             * Creates a dependency bundle provider for yaml which is an aggregate for the following dependencies:
             * <ul>
             *    <li>org.yaml:snakeyaml</li>
             * </ul>
             * This bundle was declared in catalog io.github.qupath:qupath-catalog:0.4.4
             */
            public Provider<ExternalModuleDependencyBundle> getYaml() { return createBundle("yaml"); }

        /**
         * Returns the group of bundles at bundles.opencv
         */
        public OpencvBundleAccessors getOpencv() { return baccForOpencvBundleAccessors; }

    }

    public static class OpencvBundleAccessors extends BundleFactory  implements BundleNotationSupplier{

        public OpencvBundleAccessors(ProviderFactory providers, DefaultVersionCatalog config) { super(providers, config); }

            /**
             * Creates a dependency bundle provider for opencv which is an aggregate for the following dependencies:
             * <ul>
             *    <li>org.bytedeco:javacpp</li>
             *    <li>org.bytedeco:opencv-platform</li>
             * </ul>
             * This bundle was declared in catalog io.github.qupath:qupath-catalog:0.4.4
             */
            public Provider<ExternalModuleDependencyBundle> asProvider() { return createBundle("opencv"); }

            /**
             * Creates a dependency bundle provider for opencv.cuda which is an aggregate for the following dependencies:
             * <ul>
             *    <li>org.bytedeco:javacpp</li>
             *    <li>org.bytedeco:opencv-platform-gpu</li>
             *    <li>org.bytedeco:cuda-platform</li>
             *    <li>org.bytedeco:cuda-platform-redist</li>
             * </ul>
             * This bundle was declared in catalog io.github.qupath:qupath-catalog:0.4.4
             */
            public Provider<ExternalModuleDependencyBundle> getCuda() { return createBundle("opencv.cuda"); }

            /**
             * Creates a dependency bundle provider for opencv.gpu which is an aggregate for the following dependencies:
             * <ul>
             *    <li>org.bytedeco:javacpp</li>
             *    <li>org.bytedeco:opencv-platform-gpu</li>
             *    <li>org.bytedeco:cuda-platform</li>
             * </ul>
             * This bundle was declared in catalog io.github.qupath:qupath-catalog:0.4.4
             */
            public Provider<ExternalModuleDependencyBundle> getGpu() { return createBundle("opencv.gpu"); }

    }

    public static class PluginAccessors extends PluginFactory {
        private final LicensePluginAccessors baccForLicensePluginAccessors = new LicensePluginAccessors(providers, config);

        public PluginAccessors(ProviderFactory providers, DefaultVersionCatalog config) { super(providers, config); }

            /**
             * Creates a plugin provider for javacpp to the plugin id 'org.bytedeco.gradle-javacpp-platform'
             * This plugin was declared in catalog io.github.qupath:qupath-catalog:0.4.4
             */
            public Provider<PluginDependency> getJavacpp() { return createPlugin("javacpp"); }

            /**
             * Creates a plugin provider for javafx to the plugin id 'org.openjfx.javafxplugin'
             * This plugin was declared in catalog io.github.qupath:qupath-catalog:0.4.4
             */
            public Provider<PluginDependency> getJavafx() { return createPlugin("javafx"); }

            /**
             * Creates a plugin provider for jpackage to the plugin id 'org.beryx.runtime'
             * This plugin was declared in catalog io.github.qupath:qupath-catalog:0.4.4
             */
            public Provider<PluginDependency> getJpackage() { return createPlugin("jpackage"); }

        /**
         * Returns the group of bundles at plugins.license
         */
        public LicensePluginAccessors getLicense() { return baccForLicensePluginAccessors; }

    }

    public static class LicensePluginAccessors extends PluginFactory {

        public LicensePluginAccessors(ProviderFactory providers, DefaultVersionCatalog config) { super(providers, config); }

            /**
             * Creates a plugin provider for license.report to the plugin id 'com.github.jk1.dependency-license-report'
             * This plugin was declared in catalog io.github.qupath:qupath-catalog:0.4.4
             */
            public Provider<PluginDependency> getReport() { return createPlugin("license.report"); }

    }

}
