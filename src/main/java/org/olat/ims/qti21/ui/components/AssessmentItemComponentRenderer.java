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
package org.olat.ims.qti21.ui.components;

import java.util.Date;

import org.olat.core.gui.components.Component;
import org.olat.core.gui.render.RenderResult;
import org.olat.core.gui.render.Renderer;
import org.olat.core.gui.render.StringOutput;
import org.olat.core.gui.render.URLBuilder;
import org.olat.core.gui.translator.Translator;
import org.olat.core.logging.OLATRuntimeException;
import org.olat.core.logging.OLog;
import org.olat.core.logging.Tracing;
import org.olat.ims.qti21.UserTestSession;
import org.olat.ims.qti21.model.CandidateItemEventType;
import org.olat.ims.qti21.model.jpa.CandidateEvent;
import org.olat.ims.qti21.ui.CandidateSessionContext;

import uk.ac.ed.ph.jqtiplus.node.content.variable.PrintedVariable;
import uk.ac.ed.ph.jqtiplus.node.item.AssessmentItem;
import uk.ac.ed.ph.jqtiplus.node.item.template.declaration.TemplateDeclaration;
import uk.ac.ed.ph.jqtiplus.node.outcome.declaration.OutcomeDeclaration;
import uk.ac.ed.ph.jqtiplus.node.result.SessionStatus;
import uk.ac.ed.ph.jqtiplus.running.ItemSessionController;
import uk.ac.ed.ph.jqtiplus.state.ItemSessionState;
import uk.ac.ed.ph.jqtiplus.types.Identifier;
import uk.ac.ed.ph.jqtiplus.value.Value;

/**
 * 
 * Initial date: 10.12.2014<br>
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 *
 */
public class AssessmentItemComponentRenderer extends AssessmentObjectComponentRenderer {
	
	private static final OLog log = Tracing.createLoggerFor(AssessmentItemComponentRenderer.class);

	@Override
	public void render(Renderer renderer, StringOutput sb, Component source, URLBuilder ubu,
			Translator translator, RenderResult renderResult, String[] args) {

		AssessmentItemComponent cmp = (AssessmentItemComponent)source;
		sb.append("<div class='qtiworks assessmentItem'>");

		ItemSessionController itemSessionController = cmp.getItemSessionController();
		
		CandidateSessionContext candidateSessionContext = cmp.getCandidateSessionContext();

        /* Create appropriate options that link back to this controller */
		final UserTestSession candidateSession = candidateSessionContext.getCandidateSession();
        if (candidateSession != null && candidateSession.isExploded()) {
            renderExploded(sb);
        } else if (candidateSessionContext.isTerminated()) {
            renderTerminated(sb);
        } else {
            /* Look up most recent event */
            final CandidateEvent latestEvent = candidateSessionContext.getLastEvent();// assertSessionEntered(candidateSession);

            /* Load the ItemSessionState */
            final ItemSessionState itemSessionState = cmp.getItemSessionController().getItemSessionState();// candidateDataService.loadItemSessionState(latestEvent);

            /* Touch the session's duration state if appropriate */
            if (itemSessionState.isEntered() && !itemSessionState.isEnded() && !itemSessionState.isSuspended()) {
                final Date timestamp = candidateSessionContext.getCurrentRequestTimestamp();
                itemSessionController.touchDuration(timestamp);
            }

            /* Render event */
            AssessmentRenderer renderHints = new AssessmentRenderer(renderer);
            renderItemEvent(renderHints, sb, cmp, latestEvent, itemSessionState, ubu, translator);
        }
		
		sb.append("</div>");
	}
	
    private void renderExploded(StringOutput sb) {
		sb.append("<h1>Exploded <small>say the renderer</small></h1>");
    }

    private void renderTerminated(StringOutput sb) {
		sb.append("<h1>Terminated <small>say the renderer</small></h1>");
    }
	
    private void renderItemEvent(AssessmentRenderer renderer, StringOutput sb, AssessmentItemComponent component,
    		CandidateEvent candidateEvent, ItemSessionState itemSessionState, URLBuilder ubu, Translator translator) {
        
    	final CandidateItemEventType itemEventType = candidateEvent.getItemEventType();

        /* Create and partially configure rendering request */
        //renderingRequest.setPrompt("" /* itemDeliverySettings.getPrompt() */);

        /* If session has terminated, render appropriate state and exit */
        if (itemSessionState.isExited()) {
        	renderTerminated(sb);
            return;
        }

        /* Detect "modal" events. These will cause a particular rendering state to be
         * displayed, which candidate will then leave.
         */

        if (itemEventType==CandidateItemEventType.SOLUTION) {
        	renderer.setSolutionMode(true);
        }

        /* Now set candidate action permissions depending on state of session */
        if (itemEventType==CandidateItemEventType.SOLUTION || itemSessionState.isEnded()) {
            /* Item session is ended (closed) */
        	renderer.setEndAllowed(false);
        	renderer.setHardResetAllowed(false /* itemDeliverySettings.isAllowHardResetWhenEnded() */);
        	renderer.setSoftResetAllowed(false /* itemDeliverySettings.isAllowSoftResetWhenEnded() */);
        	renderer.setSolutionAllowed(true /* itemDeliverySettings.isAllowSolutionWhenEnded() */);
        	renderer.setCandidateCommentAllowed(false);
        } else if (itemSessionState.isOpen()) {
            /* Item session is open (interacting) */
        	renderer.setEndAllowed(true /* itemDeliverySettings.isAllowEnd() */);
        	renderer.setHardResetAllowed(false /* itemDeliverySettings.isAllowHardResetWhenOpen() */);
        	renderer.setSoftResetAllowed(false /* itemDeliverySettings.isAllowSoftResetWhenOpen() */);
        	renderer.setSolutionAllowed(true /* itemDeliverySettings.isAllowSolutionWhenOpen() */);
        	renderer.setCandidateCommentAllowed(false /* itemDeliverySettings.isAllowCandidateComment() */);
        } else {
            throw new OLATRuntimeException("Item has not been entered yet. We do not currently support rendering of this state.", null);
        }

        /* Finally pass to rendering layer */
       // candidateAuditLogger.logItemRendering(candidateEvent);
        //final List<CandidateEventNotification> notifications = candidateEvent.getNotifications();
        try {
        	renderTestItemBody(renderer, sb, component, itemSessionState, ubu, translator);
        } catch (final RuntimeException e) {
            /* Rendering is complex and may trigger an unexpected Exception (due to a bug in the XSLT).
             * In this case, the best we can do for the candidate is to 'explode' the session.
             * See bug #49.
             */
        	e.printStackTrace();
        	log.error("", e);
            renderExploded(sb);
        }
    }
    
	private void renderTestItemBody(AssessmentRenderer renderer, StringOutput sb, AssessmentItemComponent component, ItemSessionState itemSessionState,
			URLBuilder ubu, Translator translator) {
		
		final AssessmentItem assessmentItem = component.getAssessmentItem();

		//title + status
		sb.append("<h1 class='itemTitle'>");
		renderItemStatus(renderer, sb, itemSessionState);
		sb.append(assessmentItem.getTitle()).append("</h1>");
		sb.append("<div id='itemBody'>");
		
		//TODO prompt
	

		//render itemBody
		assessmentItem.getItemBody().getBlocks().forEach((block)
				-> renderBlock(renderer, sb, component, assessmentItem, itemSessionState, block, ubu, translator));

		//comment
		renderComment(renderer, sb, component, itemSessionState, translator);
				
		//submit button
		if(component.isItemSessionOpen(itemSessionState, renderer.isSolutionMode())) {
			Component submit = component.getQtiItem().getSubmitButton().getComponent();
			submit.getHTMLRendererSingleton().render(renderer.getRenderer(), sb, submit, ubu, translator, new RenderResult(), null);
		}

		//end body
		sb.append("</div>");

		// Display active modal feedback (only after responseProcessing)
		if(itemSessionState.getSessionStatus() == SessionStatus.FINAL) {
			renderTestItemModalFeedback(renderer, sb, component, assessmentItem, itemSessionState, ubu, translator);
		}
	}
    
	private void renderItemStatus(AssessmentRenderer renderer, StringOutput sb, ItemSessionState itemSessionState) {
		if(renderer.isSolutionMode()) {
			sb.append("<span class='itemStatus review'>Model Solution</span>");
		} else {
			super.renderItemStatus(sb, itemSessionState);
		}
	}
	
	@Override
	protected void renderPrintedVariable(AssessmentRenderer renderer, StringOutput sb, AssessmentObjectComponent component, AssessmentItem assessmentItem, ItemSessionState itemSessionState,
			PrintedVariable printedVar) {

		Identifier identifier = printedVar.getIdentifier();
		Value templateValue = itemSessionState.getTemplateValues().get(identifier);
		Value outcomeValue = itemSessionState.getOutcomeValues().get(identifier);
		
		sb.append("<span class='printedVariable'>");
		if(outcomeValue != null) {
			OutcomeDeclaration outcomeDeclaration = assessmentItem.getOutcomeDeclaration(identifier);
			renderPrintedVariable(renderer, sb, printedVar, outcomeDeclaration, outcomeValue);
		} else if(templateValue != null) {
			TemplateDeclaration templateDeclaration = assessmentItem.getTemplateDeclaration(identifier);
			renderPrintedVariable(renderer, sb, printedVar, templateDeclaration, templateValue);
		} else {
			sb.append("(variable ").append(identifier.toString()).append(" was not found)");
		}
		sb.append("</span>");
	}
}