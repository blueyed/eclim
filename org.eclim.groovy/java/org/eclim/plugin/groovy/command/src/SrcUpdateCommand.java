/**
 * Copyright (C) 2014
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.eclim.plugin.groovy.command.src;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;

import org.eclim.annotation.Command;

import org.eclim.command.CommandLine;
import org.eclim.command.Error;
import org.eclim.command.Options;

import org.eclim.plugin.core.command.AbstractCommand;

import org.eclim.plugin.core.util.ProjectUtils;

import org.eclim.plugin.jdt.util.JavaUtils;

import org.eclim.util.file.FileOffsets;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IncrementalProjectBuilder;

import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.core.compiler.IProblem;

/**
 * Command that updates the requested groovy src file.
 */
@Command(
  name = "groovy_src_update",
  options =
    "REQUIRED p project ARG," + 
    "REQUIRED f file ARG," + 
    "OPTIONAL v validate NOARG," + 
    "OPTIONAL b build NOARG"
)
public final class SrcUpdateCommand
  extends AbstractCommand
{
  @Override
  public Object execute(CommandLine commandLine)
    throws Exception
  {
    String file = commandLine.getValue(Options.FILE_OPTION);
    String projectName = commandLine.getValue(Options.PROJECT_OPTION);
    IProject project = ProjectUtils.getProject(projectName, true);
    IFile ifile = ProjectUtils.getFile(project, file);

    // validate the src file.
    if(commandLine.hasOption(Options.VALIDATE_OPTION)){
      ICompilationUnit src =
        JavaCore.createCompilationUnitFrom(ifile);
      IProblem[] problems = JavaUtils.getProblems(src);
      List<Error> errors = new ArrayList<Error>();
      String filename = src.getResource().getLocation()
          .toOSString().replace('\\', '/');
      FileOffsets offsets = FileOffsets.compile(filename);

      for(IProblem problem : problems){
        // exclude TODO, etc
        if (problem.getID() == IProblem.Task){
          continue;
        }

        int[] lineColumn = offsets.offsetToLineColumn(problem.getSourceStart());
        errors.add(new Error(
            problem.getMessage(),
            filename,
            lineColumn[0],
            lineColumn[1],
            problem.isWarning()));
      }

      if(commandLine.hasOption(Options.BUILD_OPTION)){
        project.build(
            IncrementalProjectBuilder.INCREMENTAL_BUILD,
            new NullProgressMonitor());
      }
      return errors;
    }

    return StringUtils.EMPTY;
  }
}
