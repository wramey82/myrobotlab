/**
 *                    
 * @author greg (at) myrobotlab.org
 *  
 * This file is part of MyRobotLab (http://myrobotlab.org).
 *
 * MyRobotLab is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version (subject to the "Classpath" exception
 * as provided in the LICENSE.txt file that accompanied this code).
 *
 * MyRobotLab is distributed in the hope that it will be useful or fun,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * All libraries in thirdParty bundle are subject to their own license
 * requirements - please refer to http://myrobotlab.org/libraries for 
 * details.
 * 
 * Enjoy !
 * 
 * */

package org.myrobotlab.fileLib;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FindFile {

	boolean ShortFileName_ = false; // 8.3 format todo depricate?
	boolean IncludeDirs_ = false; // include diretories in list of files
	boolean Recurse_ = true; // recurse through sub directories
	boolean CaseInsensitive_ = true; // compares are insensitive
	String Root_ = "."; // the starting point for the search
	String SearchCriteria_ = "^*"; // search criteria for files

	List<File> FileList_ = new ArrayList<File>();

	public FindFile(final String SearchCriteria) throws IOException {
		SearchCriteria_ = SearchCriteria;
	}

	public void find() throws IOException {
		find(SearchCriteria_, Root_);
	}

	public void find(final String SearchCriteria) throws IOException {
		find(SearchCriteria, Root_);
	}

	public void find(final String SearchCriteria, final String Root)
			throws IOException {
		java.io.File f = new java.io.File(Root);
		find(f, SearchCriteria);
	}

	public void find(java.io.File dir, String name) throws IOException {
		Pattern pattern = Pattern.compile(name);

		if (dir == null)
			return;
		java.io.File[] files = dir.listFiles();
		if (files == null)
			throw new IOException("not a valid directory");
		for (int i = 0; i < files.length; ++i) {
			System.out.println(files[i].getCanonicalPath());
			// if (files[i].getName().compareToIgnoreCase(name) == 0)
			Matcher matcher = pattern.matcher(files[i].getName());
			if (matcher.find()) {
				if ((!files[i].isDirectory())
						|| (files[i].isDirectory() && IncludeDirs_)) {
					File f = new File();
					f.filename = files[i].getName();
					f.path = files[i].getPath();
					f.modified = files[i].lastModified();
					f.size = (int) files[i].length();
					FileList_.add(f);
				}

				// System.out.println("match: " + files[i].getCanonicalPath());
			}
			if (files[i].isDirectory() && Recurse_)
				find(files[i], name);
		}
	}

	public String getRoot() {
		return Root_;
	}

	public void setRoot(String root_) {
		Root_ = root_;
	}

	public String getSearchCriteria() {
		return SearchCriteria_;
	}

	public void setSearchCriteria(String searchCriteria_) {
		SearchCriteria_ = searchCriteria_;
	}

	public boolean isIncludeDirs() {
		return IncludeDirs_;
	}

	public void setIncludeDirs(boolean includeDirs_) {
		IncludeDirs_ = includeDirs_;
	}

	public boolean isRecurse() {
		return Recurse_;
	}

	public void setRecurse(boolean recurse_) {
		Recurse_ = recurse_;
	}

	public boolean isCaseInsensitive() {
		return CaseInsensitive_;
	}

	public void setCaseInsensitive(boolean caseInsensitive_) {
		CaseInsensitive_ = caseInsensitive_;
	}

	public List<File> getFileList() {
		return FileList_;
	}

	public void setFileList(List<File> fileList_) {
		FileList_ = fileList_;
	}

	/*
	 * public int FindInFile(final String match, boolean CaseSensitive, boolean
	 * DisregardWhiteSpace) {
	 * 
	 * }
	 * 
	 * public int AddFile(final File newfile) {
	 * 
	 * } public int AddDir(final String Path) {
	 * 
	 * }
	 * 
	 * public int FillFileInfo(final String DirPath, File file) {
	 * 
	 * }
	 */
}
