/**
 * This file is part of gwt-gradle-plugin.
 *
 * gwt-gradle-plugin is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * gwt-gradle-plugin is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the
 * implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser
 * General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with gwt-gradle-plugin. If not,
 * see <http://www.gnu.org/licenses/>.
 */
package de.esoco.gwt.gradle.helper;

import de.esoco.gwt.gradle.GwtLibPlugin;
import de.esoco.gwt.gradle.action.JavaAction;
import de.esoco.gwt.gradle.extension.DevOption;
import de.esoco.gwt.gradle.extension.GwtExtension;
import de.esoco.gwt.gradle.util.ResourceUtils;

import java.io.File;
import java.util.Collection;
import java.util.List;

import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.DependencySet;
import org.gradle.api.artifacts.ProjectDependency;
import org.gradle.api.file.FileCollection;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.SourceSet;

import com.google.common.collect.Lists;

public class CodeServerBuilder extends JavaCommandBuilder {

	public CodeServerBuilder() {
		setMainClass("com.google.gwt.dev.codeserver.CodeServer");
	}

	public void configure(Project project, DevOption devOption, Collection<String> modules) {
		ConfigurationContainer configs = project.getConfigurations();
		Configuration sdmConf = configs.getByName(GwtLibPlugin.CONF_GWT_SDK);

		GwtExtension extension = project.getExtensions().getByType(GwtExtension.class);

		SourceSet mainSourceSet = project.getConvention()
			.getPlugin(JavaPluginConvention.class).getSourceSets().getByName(SourceSet.MAIN_SOURCE_SET_NAME);
		FileCollection sources = project.files(mainSourceSet.getAllJava().getSrcDirs());

		configureJavaArgs(devOption);

		addClassPath(mainSourceSet.getOutput().getAsPath());
		addClassPath(mainSourceSet.getAllJava().getSrcDirs());
		addClassPath(mainSourceSet.getCompileClasspath().getAsPath());
		addClassPath(sdmConf.getAsPath());

		addSrc(sources);
		addSrc(listProjectDepsSrcDirs(project));

		addArg("-bindAddress", devOption.getBindAddress());
		addArgIf(devOption.getFailOnError(), "-failOnError", "-nofailOnError");
		addArgIf(devOption.getPrecompile(), "-precompile", "-noprecompile");
		addArg("-port", devOption.getPort());
		addArgIf(devOption.getStrict(), "-strict");
		addArgIf(devOption.getEnforceStrictResources(), "-XenforceStrictResources ", "-XnoenforceStrictResources");
		addArg("-workDir", ResourceUtils.ensureDir(devOption.getWorkDir()));
		addArgIf(devOption.getIncremental(), "-incremental", "-noincremental");
		addArg("-sourceLevel", devOption.getSourceLevel());
		if (!extension.getGwtVersion().startsWith("2.6")) {
			addArg("-logLevel", devOption.getLogLevel());
		}
		addArg("-XmethodNameDisplayMode", devOption.getMethodNameDisplayMode());
		addArg("-XjsInteropMode", devOption.getJsInteropMode());
		addArgIf(devOption.getGenerateJsInteropExports(), "-generateJsInteropExports");

		if (devOption.getExtraArgs() != null) {
			for (String arg : devOption.getExtraArgs()) {
				if (arg != null && arg.length() > 0) {
					addArg(arg);
				}
			}
		}

		for (String module : modules) {
			addArg(module);
		}
	}

	public JavaAction buildJavaAction() {
		return new JavaAction(this.toString());
	}

	private Collection<File> listProjectDepsSrcDirs(Project project) {
		ConfigurationContainer configs = project.getConfigurations();
		Configuration compileConf = configs.getByName(JavaPlugin.COMPILE_CLASSPATH_CONFIGURATION_NAME);
		DependencySet depSet = compileConf.getAllDependencies();

		List<File> result = Lists.newArrayList();
		for (Dependency dep : depSet) {
			if (dep instanceof ProjectDependency) {
				Project projectDependency = ((ProjectDependency) dep).getDependencyProject();
				if (projectDependency.getPlugins().hasPlugin(GwtLibPlugin.class)) {
					JavaPluginConvention javaConvention = projectDependency.getConvention().getPlugin(JavaPluginConvention.class);
					SourceSet mainSourceSet = javaConvention.getSourceSets().getByName(SourceSet.MAIN_SOURCE_SET_NAME);

					result.addAll(mainSourceSet.getAllSource().getSrcDirs());
				}
			}
		}
		return result;
	}

	private void addSrc(Iterable<File> sources) {
		for (File srcDir : sources) {
			if (srcDir.isDirectory()) {
				addArg("-src", srcDir);
			}
		}
	}

}
