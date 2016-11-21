package catena.model.rule;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import catena.evaluator.PairEvaluator;
import catena.model.feature.CausalSignalList;
import catena.model.feature.EventEventFeatureVector;
import catena.model.feature.EventTimexFeatureVector;
import catena.model.feature.PairFeatureVector;
import catena.model.feature.TemporalSignalList;
import catena.model.feature.FeatureEnum.PairType;
import catena.parser.TXPParser;
import catena.parser.TimeMLParser;
import catena.parser.TXPParser.Field;
import catena.parser.entities.Doc;
import catena.parser.entities.Entity;
import catena.parser.entities.EntityEnum;
import catena.parser.entities.Event;
import catena.parser.entities.Sentence;
import catena.parser.entities.TemporalRelation;
import catena.parser.entities.Timex;
import edu.stanford.nlp.parser.lexparser.LexicalizedParser;

public class TestEventEventRelationRuleTempEval3 {
	
	public TestEventEventRelationRuleTempEval3() {
		
	}
	
	public List<String> getEventEventTlinksPerFile(TXPParser txpParser, TimeMLParser tmlParser, 
			File txpFile, File tmlFile) throws Exception {
		List<String> ee = new ArrayList<String>();
		
		Doc docTxp = txpParser.parseDocument(txpFile.getPath());
		Doc docTml = tmlParser.parseDocument(tmlFile.getPath());
		
		TemporalSignalList tsignalList = new TemporalSignalList(EntityEnum.Language.EN);
		CausalSignalList csignalList = new CausalSignalList(EntityEnum.Language.EN);
	    
		//for (TemporalRelation tlink : docTxp.getTlinks()) {	//for every TLINK in TXP file: candidate pairs
		for (TemporalRelation tlink : docTml.getTlinks()) {	//for every TLINK in TML file: gold annotated pairs
			if (!tlink.getSourceID().equals(tlink.getTargetID())
					&& docTxp.getEntities().containsKey(tlink.getSourceID())
					&& docTxp.getEntities().containsKey(tlink.getTargetID())
					&& !tlink.getRelType().equals("NONE")
					) {	//classifying the relation task
				
				Entity e1 = docTxp.getEntities().get(tlink.getSourceID());
				Entity e2 = docTxp.getEntities().get(tlink.getTargetID());
				PairFeatureVector fv = new PairFeatureVector(docTxp, e1, e2, tlink.getRelType(), tsignalList, csignalList);	
				
				if (fv.getPairType().equals(PairType.event_event)) {
					EventEventFeatureVector eefv = new EventEventFeatureVector(fv);
					EventEventRelationRule eeRule = new EventEventRelationRule((Event) eefv.getE1(), (Event) eefv.getE2(), 
							docTxp, eefv.getMateDependencyPath());
					if (!eeRule.getRelType().equals("O")) {
						ee.add(eefv.getE1().getID() + "\t" + eefv.getE2().getID() + "\t" + 
								eefv.getLabel() + "\t" + eeRule.getRelType());
					} else {
						ee.add(eefv.getE1().getID() + "\t" + eefv.getE2().getID() + "\t" + 
								eefv.getLabel() + "\tNONE");
					}
				}
			}
		}
		return ee;
	}
	
	public List<String> getEventEventTlinks(TXPParser txpParser, TimeMLParser tmlParser, 
			String txpDirpath, String tmlDirpath) throws Exception {
		File[] txpFiles = new File(txpDirpath).listFiles();
		List<String> et = new ArrayList<String>();
		
		for (File txpFile : txpFiles) {	//assuming that there is no sub-directory
			File tmlFile = new File(tmlDirpath, txpFile.getName().replace(".txp", ""));
			List<String> etPerFile = getEventEventTlinksPerFile(txpParser, tmlParser, txpFile, tmlFile);
			et.addAll(etPerFile);
			PairEvaluator pe = new PairEvaluator(etPerFile);
			pe.printIncorrectAndSentence(txpParser, txpFile);
		}		
		return et;
	}

	public static void main(String [] args) throws Exception {
		Field[] fields = {Field.token, Field.token_id, Field.sent_id, Field.pos, 
				Field.lemma, Field.deps, Field.tmx_id, Field.tmx_type, Field.tmx_value, 
				Field.ner, Field.ev_class, Field.ev_id, Field.role1, Field.role2, 
				Field.role3, Field.is_arg_pred, Field.has_semrole, Field.chunk, 
				Field.main_verb, Field.connective, Field.morpho, 
				Field.tense_aspect_pol, /*Field.coref_event,*/ Field.tlink};
		TXPParser txpParser = new TXPParser(EntityEnum.Language.EN, fields);		
		TimeMLParser tmlParser = new TimeMLParser(EntityEnum.Language.EN);
		
//		String txpDirpath = "./data/TempEval3-train_TXP2/";
//		String tmlDirpath = "./data/TempEval3-train_TML/";
		
		String txpDirpath = "./data/TempEval3-eval_TXP/";
		String tmlDirpath = "./data/TempEval3-eval_TML/";
		
		File[] txpFiles = new File(txpDirpath).listFiles();		
		if (txpFiles == null) return;	
		
		TestEventEventRelationRuleTempEval3 test = new TestEventEventRelationRuleTempEval3();
		List<String> eeResult = test.getEventEventTlinks(txpParser, tmlParser, txpDirpath, tmlDirpath);

		PairEvaluator pe = new PairEvaluator(eeResult);
		pe.evaluatePerLabel();   
		
	}
}