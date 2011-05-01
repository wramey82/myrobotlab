package org.myrobotlab.speech;

import java.io.IOException;

/**
 * A Dialog node behavior that loads a completely new grammar upon entry into
 * the node
 */
public class NewGrammarDialogNodeBehavior extends DialogNodeBehavior {

	/**
	 * creates a NewGrammarDialogNodeBehavior
	 * 
	 * @param grammarName
	 *            the grammar name
	 */
	public NewGrammarDialogNodeBehavior() {
	}

	/**
	 * Called with the dialog manager enters this entry
	 */
	public void onEntry() throws IOException {
		super.onEntry();
		getGrammar().loadJSGF(getGrammarName());
	}

	/**
	 * Returns the name of the grammar. The name of the grammar is the same as
	 * the name of the node
	 * 
	 * @return the grammar name
	 */
	public String getGrammarName() {
		return getName();
	}
}
