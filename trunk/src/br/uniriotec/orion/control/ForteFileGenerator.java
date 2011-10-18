package br.uniriotec.orion.control;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import br.uniriotec.orion.model.forte.resources.Concept;
import br.uniriotec.orion.model.forte.resources.IExample;
import br.uniriotec.orion.model.forte.resources.Relationship;

/**
 * Classe para gerar os arquivos que serão utilizados como input pelo forte com base 
 * na ontologia fornecida pelo usuário. Esta classe faz uso da classe ForteDataGenerator
 * para gerar os dados que serão inseridos nos arquivos.
 * 
 * @author Felipe
 *
 */
public class ForteFileGenerator {
	
	private ForteDataGenerator dataGenerator;
	
	public ForteFileGenerator(String inputFile){
		this.dataGenerator = new ForteDataGenerator(inputFile);
	}
	
    /**
     * Gera o arquivo THY, que comporta as regras que compõe a teoria. As regras do 
     * arquivo THY são todas as regras que foram selecionadas pelo usuário para revisão
     * somadas aos predicados identificados como "intermediate_predicates", ou seja, que
     * são utilizados no corpo das regras selecionadas para revisão.
     * 
     * OBS: Predicados no corpo de regras que estão definidos no arquivo FDT receberão
     *  o prefixo "fdt:".
     * 
     * @param rulesForRevision
     * @throws IOException 
     */
    public void generateTheoryRules(List<Concept> rulesForRevision) throws IOException{
    	List<Concept> conceitosRevisaveis = rulesForRevision;
    	conceitosRevisaveis.addAll(dataGenerator.generateIntermediatePredicates(rulesForRevision));
    	
    	//Criar lista com todos os predicados que não requisitam o prefixo "fdt:"
    	List<String> cabecaPredicadosTHY = new ArrayList<String>();
    	for(Concept c : conceitosRevisaveis){
    		cabecaPredicadosTHY.add(c.getNome()+"(A)");
    	}
    	
    	//Preparar arquivo THY para escrita
    	BufferedWriter writter = new BufferedWriter(new FileWriter("src/input/forte/times.thy"));
    	
        for(Concept c : conceitosRevisaveis){
        	/* verificar se os predicados no corpo das regras fazem parte do "rulesForRevision"
        	 * ou do intermediatePredicates, senão, inserir "fdt:", pois o predicado se encontra
        	 * definido no arquivo FDT.
        	 */
        	int posSinalImplicacao = c.toString().indexOf(":-");
        	String cabecaRegra = c.toString().substring(0, posSinalImplicacao-1);
        	String corpoRegra = c.toString().substring(posSinalImplicacao+3, c.toString().length());
        	String corpoRegraPrefixado = "";
        	
        	String[] arrayPredicadosCorpoRegra = corpoRegra.split(", ");
        	
        	for(String s : arrayPredicadosCorpoRegra){
        		boolean isCabeca = false;
        		for(String cabecaPred : cabecaPredicadosTHY){
        			if(cabecaPred.equals(s)){
        				isCabeca = true;
            		}	
        		}
        		if(isCabeca){
        			corpoRegraPrefixado += s+", ";
        		}else{
        			corpoRegraPrefixado += "fdt:"+s+", ";
        		}	
        	}
        	
        	//trocar ultimo ", " por "."
        	corpoRegraPrefixado = corpoRegraPrefixado.substring(0, corpoRegraPrefixado.length()-2);
        	
        	//Escrever a regra no arquivo
            writter.append(cabecaRegra+" :- "+corpoRegraPrefixado+"\n");
        }
        writter.flush();
    }
    
    /** 
     * Gera o arquivo FDT, que informa ao FORTE o conhecimento fundamental sobre a teoria
     * que será revisada. O arquivo FDT deverá comportar conceitos tidos como não
     * revisáveis e todas as regras originadas de Relacionamentos (Relationship).
     * Algumas regras revisáveis podem ser excluidas da revisão pelo usuário, neste caso
     * devem ser adicionadas ao FDT.
     * 
     * @throws IOException 
     */
    public void generateFundamentalTheory(List<Concept> rulesForRevision) throws IOException{
        List<Concept> conceitosPrimeiroNivel = dataGenerator.retrieveFirstLevelConcepts();
    	List<Concept> conceitosNegativos = dataGenerator.retrieveNegativeConcepts();
        List<Concept> conceitosRevisaveis = dataGenerator.retrieveRevisableConcepts();
        List<Relationship> relacionamentos = dataGenerator.generateRelationships();
        
        //Lista de conceitos revisaveis que o usuario optou por nao revisar
        //Consites no conjunto de todos os conceitos revisaveis menos os selecionados
        //para revisao e os intermediarios dos selecionados para revisao.
        List<Concept> conceitosRevisaveisExcluidos = conceitosRevisaveis;
        //exclui os selecionados
        conceitosRevisaveisExcluidos.removeAll(rulesForRevision);
        //exclui os intermediarios dos conceitos selecionados
        conceitosRevisaveisExcluidos.removeAll(dataGenerator.generateIntermediatePredicates(rulesForRevision));
        
        String moduloFDT = ":- module(fdt, [";
        Set<String> itensModule = new HashSet<String>();
        String conceitosFDT = "\n\n";
        String relacionamentosFDT = "\n\n";
        
        //Escrever conceitos Não revisáveis (Conceitos de primeiro nivel)
        for(Concept c : conceitosPrimeiroNivel){
            conceitosFDT += c.getNome() + "(X) :- example(" + c.getNome() + "(X)" + ").\n";
            itensModule.add(c.getNome() + "/1, ");
        }
        
        //Escrever conceitos negativos gerados pelo axioma disjointWith
        for(Concept c : conceitosNegativos){
        	if(conceitosFDT.contains(c.toString()) == false){
        		conceitosFDT += c.toString()+"\n";
                itensModule.add(c.getNome() + "/1, ");
        	}
        }
        
        //Escrever Conceitos revisaveis excluidos da revisão
        for(Concept c : conceitosRevisaveisExcluidos){
        	if(conceitosFDT.contains(c.toString()) == false){
        		conceitosFDT += c.toString()+"\n";
                itensModule.add(c.getNome() + "/1, ");
        	}
        }
        
        //Escrever relacionamentos
        for(Relationship r : relacionamentos){
            relacionamentosFDT += r.toString()+".\n";
            itensModule.add(r.getNome() + "/2, ");
        }
        
        //inserir todos os itensModule em moduloFDT
        for(String s : itensModule){
        	moduloFDT += s;
        }
        
        moduloFDT = moduloFDT.substring(0, moduloFDT.length()-2) + "]).";
        
        BufferedWriter writter = new BufferedWriter(new FileWriter("src/input/forte/times.fdt"));
        writter.append(moduloFDT);
        writter.append(conceitosFDT);
        writter.append(relacionamentosFDT);
        writter.flush();
    }
    
    /**
     * Gera o arquivo DAT, que informa ao FORTE os seguintes dados básicos:
     *  - top_level_predicates;
     *  - intermediate_predicates;
     *  - strata (deixar em branco);
     *  - shielded (contem todos os predicados da teoria que não serão revisados);
     *  - object_attributes;
     *  - object_relations (relacionamentos usados ocmo predicados intermediarios definidos no FDT);
     *  - language_bias (usando padrão);
     *  - example (positivo, negativo, Objects, fatos)
     * 
     * @throws IOException 
     */
    public void generateDataFile(List<Concept> rulesForRevision) throws IOException{
        List<Concept> topLevelConcepts = rulesForRevision;
    	List<Concept> intermediateConcepts = dataGenerator.generateIntermediatePredicates(rulesForRevision);
    	List<Relationship> relacionamentos = dataGenerator.generateRelationships();
    	
    	List<String> topLevelPredicates = new ArrayList<String>();
    	List<String> intermediatePredicates = new ArrayList<String>();
    	Set<String> variaveis = new HashSet<String>();
    	//Unir as regras selecionadas para revisao (top_level + intermediates)
    	List<Concept> regrasParaRevisao = rulesForRevision;
    	regrasParaRevisao.addAll(dataGenerator.generateIntermediatePredicates(rulesForRevision));
    	
    	
    	//Recuperar predicados de top_level e intermediate.
    	//recupera o nome do predicado e a sua superclasse, gera-se entao uma variavel
    	for(Concept c: topLevelConcepts){
    		String variavel = "var"+c.getAxiomas().get(0).getValor();
    		topLevelPredicates.add(c.getNome()+ "(" + variavel + ")");
    		variaveis.add(variavel);
    	}
    	for(Concept c: intermediateConcepts){
    		String variavel = "var"+c.getAxiomas().get(0).getValor();
    		intermediatePredicates.add(c.getNome()+ "(" + variavel + ")");
    		variaveis.add(variavel);
    	}
    	
    	/*======================================+
    	 * 		Preparar Texto para escrita		*
    	 *======================================*/
    	
    	//Preparar Top Level Predicate
    	String top_level = "top_level_predicates([";
    	for(String s : topLevelPredicates){
    		top_level += s+", ";
        }
    	top_level = top_level.substring(0, top_level.length()-2) + "]).";
    	
    	//Preparar Intermediate Predicate
    	String intermediate = "intermediate_predicates([";
    	for(String s : intermediatePredicates){
    		intermediate += s+", ";
        }
    	intermediate = intermediate.substring(0, intermediate.length()-2) + "]).";

    	//Preparar Strata
    	String strata = "strata([]).";
    	
    	//Preparar Shielded
    	String shielded = "shielded([";
    	List<Concept> conjuntoRegrasNaoRevisadas = dataGenerator.retrieveRevisableConcepts();
    	conjuntoRegrasNaoRevisadas.removeAll(rulesForRevision);
    	for(Concept c : conjuntoRegrasNaoRevisadas){
    		shielded += c.getNome()+"(_), ";
    	}
    	shielded = shielded.substring(0, shielded.length()-2) + "]).";
    	
    	//Preparar Object Relations
    	//TODO conferir se a composição do elemento está certa (é pra entrar OP mesmo?)
    	String objectRel = "object_relations([";
    	for(Relationship r : relacionamentos){
    		String v1 = "var" + dataGenerator.lowerFirstChar(r.getPrimeiroTermo().get(0));
    		String v2 = "var" + dataGenerator.lowerFirstChar(r.getSegundoTermo());
    		objectRel += r.getNome()+ "(" + v1 + "," + v2 + "), ";
    		variaveis.add(v1);
    		variaveis.add(v2);
    	}
    	objectRel = objectRel.substring(0, objectRel.length()-2) + "]).";
    	
    	//Preparar Object Attribute
    	String objectAttr = "object_attributes([";
    	for(String v : variaveis){
    		objectAttr += v+"([]), ";
    	}
    	objectAttr = objectAttr.substring(0, objectAttr.length()-2) + "]).";
    	
    	
    	//Preparar Exemplos Positivos
    	List<IExample> exemplosPositivosList = dataGenerator.generatePositiveExamples(regrasParaRevisao);
    	String exemplosPositivos = "[";
    	if(exemplosPositivosList.size()!=0){
    		for(IExample ex : exemplosPositivosList){
        		exemplosPositivos += ex.toString()+", ";
        	}
        	exemplosPositivos = exemplosPositivos.substring(0, exemplosPositivos.length()-2);
    	}
    	exemplosPositivos += "]";
    	
    	//Preparar Exemplos negativos
    	List<IExample> exemplosNegativosList = dataGenerator.generateNegativeExamples(regrasParaRevisao);
    	String exemplosNegativos = "[";
    	if(exemplosNegativosList.size()!=0){
    		for(IExample ex : exemplosNegativosList){
        		exemplosNegativos += ex.toString()+", ";
        	}
    		exemplosNegativos = exemplosNegativos.substring(0, exemplosNegativos.length()-2);
    	}
    	exemplosNegativos += "]";
    	
    	//Preparar Objects
    	/* 
    	 * REVER TODA ESTA IDEIA APOS REUNIAO COM A KATE SOBRE A IDEIA DO "ISA"
    	 * 
    	 * - Recuperar as variaveis usadas no Object_attributes
    	 * - Para cada variavel recuperar instancias do conceito representado
    	 * - Com a instancia escreve-se [id, id_relacionado1, id_relacionado2, ...]
    	 * de acordo com a ordem descrita nas variaveis.
    	 */
    	String objects = "[";
    	for(String v : variaveis){
    		objects += v+"([]), ";
    	}
    	objects = objects.substring(0, objects.length()-2) + "]";
    	
    	//Preparar Fatos
    	List<IExample> factsList = dataGenerator.generateFacts(regrasParaRevisao);
    	String facts = "[";
    	if(factsList.size()!=0){
    		for(IExample ex : factsList){
    			facts += ex.toString()+", ";
        	}
    		facts = facts.substring(0, facts.length()-2);
    	}
    	facts += "]";
    	
    	//Realizar a escrita em arquivo
    	BufferedWriter writter = new BufferedWriter(new FileWriter("src/input/forte/times.dat"));
        writter.append(top_level+"\n\n");
        writter.append(intermediate+"\n\n");
        writter.append(strata+"\n\n");
        writter.append(shielded+"\n\n");
        writter.append(objectAttr+"\n\n");
        writter.append(objectRel+"\n\n");
        writter.append("language_bias([depth_limit(5), use_attr, use_relations, " +
        		"use_theory, use_built_in, relation_tuning(highly_relational)]).\n\n");
        //Escrever dados
        writter.append("example(\n");
        	//Exemplos Positivos
        	writter.append(exemplosPositivos+"\n,\n");
        	//Exemplos Negativos
        	writter.append(exemplosNegativos+"\n,\n");
        	//Objects
        	writter.append(objects+"\n,\n");
        	//Facts
        	writter.append("facts("+facts+")\n\n");
        writter.append(").\n\n");
        writter.flush();
    }
    
    /**
     * Gera o arquivo DOM, que informa ao FORTE os arquivos que devem ser incluídos
     * na execução do sistema para descrever o domínio da teoria.
     * 
     * @throws IOException 
     */
    public void generateDomainKnowledgeFile() throws IOException{
        BufferedWriter writter = new BufferedWriter(new FileWriter("src/input/forte/times.dom"));
        writter.append(":- compile('times.fdt').\n");
        writter.append(":- compile('default.rv').\n");
        writter.append(":- compile('default.et').\n");
        writter.append(":- compile('default.tt').\n");
        writter.flush();
    }


}
