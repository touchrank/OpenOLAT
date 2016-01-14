/**
 * <a href="http://www.openolat.org">
 * OpenOLAT - Online Learning and Training</a><br>
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); <br>
 * you may not use this file except in compliance with the License.<br>
 * You may obtain a copy of the License at the
 * <a href="http://www.apache.org/licenses/LICENSE-2.0">Apache homepage</a>
 * <p>
 * Unless required by applicable law or agreed to in writing,<br>
 * software distributed under the License is distributed on an "AS IS" BASIS, <br>
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. <br>
 * See the License for the specific language governing permissions and <br>
 * limitations under the License.
 * <p>
 * Initial code contributed and copyrighted by<br>
 * frentix GmbH, http://www.frentix.com
 * <p>
 */
package org.olat.ims.qti21.ui.editor.items;

import java.util.ArrayList;
import java.util.List;

import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.form.flexible.FormItem;
import org.olat.core.gui.components.form.flexible.FormItemContainer;
import org.olat.core.gui.components.form.flexible.elements.FormLink;
import org.olat.core.gui.components.form.flexible.elements.RichTextElement;
import org.olat.core.gui.components.form.flexible.elements.SingleSelection;
import org.olat.core.gui.components.form.flexible.elements.TextElement;
import org.olat.core.gui.components.form.flexible.impl.FormBasicController;
import org.olat.core.gui.components.form.flexible.impl.FormEvent;
import org.olat.core.gui.components.form.flexible.impl.FormLayoutContainer;
import org.olat.core.gui.components.form.flexible.impl.elements.richText.RichTextConfiguration;
import org.olat.core.gui.components.link.Link;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.util.Util;
import org.olat.ims.qti21.QTI21Constants;
import org.olat.ims.qti21.model.xml.items.KPrimChoiceAssessmentItemBuilder;
import org.olat.ims.qti21.ui.editor.AssessmentTestEditorController;
import org.olat.ims.qti21.ui.editor.events.AssessmentItemEvent;

import uk.ac.ed.ph.jqtiplus.node.item.interaction.MatchInteraction;
import uk.ac.ed.ph.jqtiplus.node.item.interaction.choice.SimpleAssociableChoice;
import uk.ac.ed.ph.jqtiplus.types.Identifier;

/**
 * KPrim is 8 simple choice, but 2 choices are paired together.
 * 
 * Initial date: 06.01.2016<br>
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 *
 */
public class KPrimChoiceEditorController extends FormBasicController {
	
	private static final String[] yesnoKeys = new String[]{ "y", "n"};
	
	private TextElement titleEl;
	private RichTextElement textEl;
	private SingleSelection shuffleEl;
	private FormLayoutContainer answersCont;
	private final List<KprimWrapper> choiceWrappers = new ArrayList<>();

	private int count = 0;
	private final KPrimChoiceAssessmentItemBuilder itemBuilder;
	
	public KPrimChoiceEditorController(UserRequest ureq, WindowControl wControl, KPrimChoiceAssessmentItemBuilder itemBuilder) {
		super(ureq, wControl, "simple_choices_editor");
		setTranslator(Util.createPackageTranslator(AssessmentTestEditorController.class, getLocale()));
		this.itemBuilder = itemBuilder;
		initForm(ureq);
	}

	@Override
	protected void initForm(FormItemContainer formLayout, Controller listener, UserRequest ureq) {
		setFormTitle("editor.kprim.title");
		
		FormLayoutContainer metadata = FormLayoutContainer.createDefaultFormLayout("metadata", getTranslator());
		metadata.setRootForm(mainForm);
		formLayout.add(metadata);
		formLayout.add("metadata", metadata);

		titleEl = uifactory.addTextElement("title", "form.imd.title", -1, itemBuilder.getTitle(), metadata);
		titleEl.setMandatory(true);
		
		String description = itemBuilder.getQuestion();
		textEl = uifactory.addRichTextElementForStringData("desc", "form.imd.descr", description, 8, -1, true, null, null,
				metadata, ureq.getUserSession(), getWindowControl());
		RichTextConfiguration richTextConfig = textEl.getEditorConfiguration();
		richTextConfig.setFileBrowserUploadRelPath("media");// set upload dir to the media dir
				
		//points -> in other controller
		
		//shuffle
		String[] yesnoValues = new String[]{ translate("yes"), translate("no") };
		shuffleEl = uifactory.addRadiosHorizontal("shuffle", "form.imd.shuffle", metadata, yesnoKeys, yesnoValues);
		if (itemBuilder.isShuffle()) {
			shuffleEl.select("y", true);
		} else {
			shuffleEl.select("n", true);
		}

		//responses
		String page = velocity_root + "/kprim_choices.html";
		answersCont = FormLayoutContainer.createCustomFormLayout("answers", getTranslator(), page);
		answersCont.setRootForm(mainForm);
		formLayout.add(answersCont);
		formLayout.add("answers", answersCont);

		MatchInteraction interaction = itemBuilder.getMatchInteraction();
		if(interaction != null) {
			List<SimpleAssociableChoice> choices = itemBuilder.getKprimChoices();
			for(SimpleAssociableChoice choice:choices) {
				wrapAnswer(ureq, choice);
			}
		}
		answersCont.contextPut("choices", choiceWrappers);
		recalculateUpDownLinks();

		// Submit Button
		FormLayoutContainer buttonsContainer = FormLayoutContainer.createButtonLayout("buttons", getTranslator());
		buttonsContainer.setRootForm(mainForm);
		formLayout.add(buttonsContainer);
		formLayout.add("buttons", buttonsContainer);
		uifactory.addFormSubmitButton("submit", buttonsContainer);
	}
	
	private void wrapAnswer(UserRequest ureq, SimpleAssociableChoice choice) {
		String choiceContent =  itemBuilder.getHtmlHelper().flowStaticString(choice.getFlowStatics());
		String choiceId = "answer" + count++;
		RichTextElement choiceEl = uifactory.addRichTextElementForStringData(choiceId, "form.imd.answer", choiceContent, 8, -1, true, null, null,
				answersCont, ureq.getUserSession(), getWindowControl());
		choiceEl.setUserObject(choice);
		answersCont.add("choiceId", choiceEl);
		
		FormLink upLink = uifactory.addFormLink("up-".concat(choiceId), "up", "", null, answersCont, Link.NONTRANSLATED);
		upLink.setIconLeftCSS("o_icon o_icon-lg o_icon_move_up");
		answersCont.add(upLink);
		answersCont.add("up-".concat(choiceId), upLink);
		
		FormLink downLink = uifactory.addFormLink("down-".concat(choiceId), "down", "", null, answersCont, Link.NONTRANSLATED);
		downLink.setIconLeftCSS("o_icon o_icon-lg o_icon_move_down");
		answersCont.add(downLink);
		answersCont.add("down-".concat(choiceId), downLink);
		
		boolean correct = itemBuilder.isCorrect(choice.getIdentifier());
		boolean wrong = itemBuilder.isWrong(choice.getIdentifier());
		choiceWrappers.add(new KprimWrapper(choice, correct, wrong, choiceEl, upLink, downLink));
	}
	
	@Override
	protected void doDispose() {
		//
	}
	
	@Override
	protected void formOK(UserRequest ureq) {
		//title
		itemBuilder.setTitle(titleEl.getValue());
		//question
		String questionText = textEl.getValue();
		itemBuilder.setQuestion(questionText);
		
		
		//shuffle
		itemBuilder.setShuffle(shuffleEl.isOneSelected() && shuffleEl.isSelected(0));
		
		//update kprims
		List<SimpleAssociableChoice> choiceList = new ArrayList<>();
		for(KprimWrapper choiceWrapper:choiceWrappers) {
			SimpleAssociableChoice choice = choiceWrapper.getSimpleChoice();
			String answer = choiceWrapper.getAnswer().getValue();
			itemBuilder.getHtmlHelper().appendHtml(choice, answer);
			choiceList.add(choice);
		}
		
		//set associations
		for(KprimWrapper choiceWrapper:choiceWrappers) {
			SimpleAssociableChoice choice = choiceWrapper.getSimpleChoice();
			Identifier choiceIdentifier = choice.getIdentifier();
			String association = ureq.getHttpReq().getParameter(choiceIdentifier.toString());
			if("correct".equals(association)) {
				itemBuilder.setAssociation(choiceIdentifier, QTI21Constants.CORRECT_IDENTIFIER);
			} else if("wrong".equals(association)) {
				itemBuilder.setAssociation(choiceIdentifier, QTI21Constants.WRONG_IDENTIFIER);
			}
			choiceWrapper.setCorrect(itemBuilder.isCorrect(choiceIdentifier));
			choiceWrapper.setWrong(itemBuilder.isWrong(choiceIdentifier));
		}

		fireEvent(ureq, new AssessmentItemEvent(AssessmentItemEvent.ASSESSMENT_ITEM_CHANGED, itemBuilder.getAssessmentItem()));
	}

	@Override
	protected void formInnerEvent(UserRequest ureq, FormItem source, FormEvent event) {
		if(source instanceof FormLink) {
			FormLink button = (FormLink)source;
			String cmd = button.getCmd();
			if("up".equals(cmd)) {
				doMoveSimpleChoiceUp((KprimWrapper)button.getUserObject());
			} else if("down".equals(cmd)) {
				doMoveSimpleChoiceDown((KprimWrapper)button.getUserObject());
			}
		}
		super.formInnerEvent(ureq, source, event);
	}

	private void doMoveSimpleChoiceUp(KprimWrapper choiceWrapper) {
		int index = choiceWrappers.indexOf(choiceWrapper) - 1;
		if(index >= 0 && index < choiceWrappers.size()) {
			choiceWrappers.remove(choiceWrapper);
			choiceWrappers.add(index, choiceWrapper);
		}
		recalculateUpDownLinks();
		flc.setDirty(true);
	}
	
	private void doMoveSimpleChoiceDown(KprimWrapper choiceWrapper) {
		int index = choiceWrappers.indexOf(choiceWrapper) + 1;
		if(index > 0 && index < choiceWrappers.size()) {
			choiceWrappers.remove(choiceWrapper);
			choiceWrappers.add(index, choiceWrapper);
		}
		recalculateUpDownLinks();
		flc.setDirty(true);
	}
	
	private void recalculateUpDownLinks() {
		int numOfChoices = choiceWrappers.size();
		for(int i=0; i<numOfChoices; i++) {
			KprimWrapper choiceWrapper = choiceWrappers.get(i);
			choiceWrapper.getUp().setEnabled(i != 0);
			choiceWrapper.getDown().setEnabled(i < (numOfChoices - 1));
		}
	}


	public static final class KprimWrapper {
		
		private final SimpleAssociableChoice choice;
		private final RichTextElement answerEl;
		private final FormLink upLink, downLink;
		
		private boolean correct;
		private boolean wrong;
		private final Identifier choiceIdentifier;
		
		public KprimWrapper(SimpleAssociableChoice choice, boolean correct, boolean wrong, RichTextElement answerEl,
				FormLink upLink, FormLink downLink) {
			this.choice = choice;
			this.correct = correct;
			this.wrong = wrong;
			this.choiceIdentifier = choice.getIdentifier();
			this.answerEl = answerEl;
			answerEl.setUserObject(this);
			this.upLink = upLink;
			upLink.setUserObject(this);
			this.downLink = downLink;
			downLink.setUserObject(this);
		}
		
		public Identifier getIdentifier() {
			return choiceIdentifier;
		}
		
		public String getIdentifierString() {
			return choiceIdentifier.toString();
		}
		
		public boolean isCorrect() {
			return correct;
		}
		
		public void setCorrect(boolean correct) {
			this.correct = correct;
		}
		
		public boolean isWrong() {
			return wrong;
		}
		
		public void setWrong(boolean wrong) {
			this.wrong = wrong;
		}
		
		public SimpleAssociableChoice getSimpleChoice() {
			return choice;
		}
		
		public RichTextElement getAnswer() {
			return answerEl;
		}

		public FormLink getUp() {
			return upLink;
		}

		public FormLink getDown() {
			return downLink;
		}
	}
}
