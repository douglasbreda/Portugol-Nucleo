package br.univali.portugol.nucleo.bibliotecas.base;

import br.univali.portugol.nucleo.Programa;
import br.univali.portugol.nucleo.asa.ModoAcesso;
import br.univali.portugol.nucleo.asa.Quantificador;
import br.univali.portugol.nucleo.asa.TipoDado;
import br.univali.portugol.nucleo.bibliotecas.base.anotacoes.DocumentacaoBiblioteca;
import br.univali.portugol.nucleo.bibliotecas.base.anotacoes.DocumentacaoConstante;
import br.univali.portugol.nucleo.bibliotecas.base.anotacoes.DocumentacaoFuncao;
import br.univali.portugol.nucleo.bibliotecas.base.anotacoes.DocumentacaoParametro;
import br.univali.portugol.nucleo.bibliotecas.base.anotacoes.NaoExportar;
import br.univali.portugol.nucleo.bibliotecas.base.anotacoes.PropriedadesBiblioteca;
import br.univali.portugol.nucleo.execucao.ObservadorExecucao;
import br.univali.portugol.nucleo.execucao.ResultadoExecucao;
import br.univali.portugol.nucleo.mensagens.ErroExecucao;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Esta classe é responsável por carregar as bibliotecas em memória e gerenciar
 * seu ciclo de vida. É responsável também por criar os metadados das bibliotecas
 * e validar se as mesmas foram implementadas de acordo com as regras definidas
 * na classe base ({@link Biblioteca}).
 * 
 * @author Luiz Fernando Noschang
 */
public final class GerenciadorBibliotecas implements ObservadorExecucao
{
    private static GerenciadorBibliotecas instance = null;

    private List<String> bibliotecasDisponiveis;
    
    private MetaDadosBibliotecas metaDadosBibliotecas;
    private Map<String, Class<? extends Biblioteca>> bibliotecasCarregadas;
    
    private Map<String, Biblioteca> bibliotecasCompartilhadas;
    private Map<Programa, Map<String, Biblioteca>> bibliotecasReservadas;
    
    
    public static GerenciadorBibliotecas getInstance()
    {
        if (instance == null)
        {
            instance = new GerenciadorBibliotecas();
        }
        
        return instance;
    }
    
    private GerenciadorBibliotecas()
    {
        bibliotecasCarregadas = new TreeMap<>();
        metaDadosBibliotecas = new MetaDadosBibliotecas();
        
        bibliotecasCompartilhadas = new TreeMap<>();        
        bibliotecasReservadas = new TreeMap<>(new ComparadorPrograma());
    }
    
    private class ComparadorPrograma implements Comparator<Programa>
    {
        @Override
        public int compare(Programa o1, Programa o2)
        {
            Integer h1 = System.identityHashCode(o1);
            Integer h2 = System.identityHashCode(o2);
            
            return h1.compareTo(h2);
        }
    }
    
    public List<String> listarBibliotecasDisponiveis()
    {
        if (bibliotecasDisponiveis == null)
        {
            bibliotecasDisponiveis = new ArrayList<>();            
            bibliotecasDisponiveis.add("Util");
            bibliotecasDisponiveis.add("Graficos");
            bibliotecasDisponiveis.add("Matematica");
            bibliotecasDisponiveis.add("Teclado");
            bibliotecasDisponiveis.add("Texto");
            bibliotecasDisponiveis.add("Tipos");
            bibliotecasDisponiveis.add("Mouse");
            
            Collections.sort(bibliotecasDisponiveis);
        }
        
        return new ArrayList<>(bibliotecasDisponiveis);
    }
    
    public void registrarBibliotecaExterna(Class<? extends Biblioteca> biblioteca) throws ErroCarregamentoBiblioteca
    {
        final String nome = biblioteca.getSimpleName();
        if (!bibliotecasCarregadas.containsKey(nome))
        {
            listarBibliotecasDisponiveis();
            bibliotecasDisponiveis.add(nome);
            bibliotecasCarregadas.put(nome, biblioteca);
            MetaDadosBiblioteca metaDadosBiblioteca = obterMetaDadosBiblioteca(nome, biblioteca);
            metaDadosBibliotecas.incluir(metaDadosBiblioteca);
        } else {
            throw new ErroCarregamentoBiblioteca(nome, "Uma biblioteca já foi registrada com este nome");
        }
    }

    /**
     * Obtém os metadados da biblioteca especificada. Os metadados contém
     * informações importantes sobre a biblioteca, como a documentação e os 
     * metadados das funções e constantes.
     * 
     * <p>
     *      A chamada a este métodos fará com que o {@link GerenciadorBibliotecas} 
     *      tente carregar a biblioteca caso ela ainda não esteja em memória.
     * </p>
     * 
     * 
     * @param nome  o nome da biblioteca para a qual se deseja obter os metadados
     * @return      os metadados da biblioteca em questão
     * 
     * @throws ErroCarregamentoBiblioteca   esta exceção é jogada caso o {@link GerenciadorBibliotecas}
     *                                      não consiga carregar a biblioteca especificada
     */
    public MetaDadosBiblioteca obterMetaDadosBiblioteca(String nome) throws ErroCarregamentoBiblioteca
    {
        if (!metaDadosBibliotecas.contem(nome))
        {
            Class classeBiblioteca = carregarBiblioteca(nome);
            MetaDadosBiblioteca metaDadosBiblioteca = obterMetaDadosBiblioteca(nome, classeBiblioteca);
    
            metaDadosBibliotecas.incluir(metaDadosBiblioteca);
        }
    
        return metaDadosBibliotecas.obter(nome);
    }
    
    /**
     * Obtém a biblioteca especificada, carregando-a se necessário. Este método é responsável
     * por gerenciar o ciclo de vida da biblioteca em memória.
     * 
     * @param nome  o nome da biblioteca a ser obtida
     * @param programa  
     * @return
     * @throws ErroCarregamentoBiblioteca 
     */
    public Biblioteca registrarBiblioteca(String nome, Programa programa) throws ErroCarregamentoBiblioteca
    {
        try
        {
            MetaDadosBiblioteca metaDadosBiblioteca = obterMetaDadosBiblioteca(nome);

            if (metaDadosBiblioteca.getTipo() == TipoBiblioteca.COMPARTILHADA)
            {
                if (!bibliotecasCompartilhadas.containsKey(nome))
                {
                    Biblioteca biblioteca =  bibliotecasCarregadas.get(nome).newInstance();
                    biblioteca.inicializar();
                    
                    bibliotecasCompartilhadas.put(nome, biblioteca);
                }
                
                return bibliotecasCompartilhadas.get(nome);
            }
            else if (metaDadosBiblioteca.getTipo() == TipoBiblioteca.RESERVADA)
            {
                if (!bibliotecasReservadas.containsKey(programa))
                {
                    bibliotecasReservadas.put(programa, new TreeMap<String, Biblioteca>());
                }
                
                Map<String, Biblioteca> memoriaPrograma = bibliotecasReservadas.get(programa);
                
                if (!memoriaPrograma.containsKey(nome))
                {
                    Biblioteca biblioteca = bibliotecasCarregadas.get(nome).newInstance();
                    biblioteca.inicializar(programa, new ArrayList<>(memoriaPrograma.values()));
                    
                    for (Biblioteca bib : memoriaPrograma.values())
                    {
                        bib.bibliotecaRegistrada(biblioteca);
                    }
                    
                    memoriaPrograma.put(nome, biblioteca);
                }
                
                return memoriaPrograma.get(nome);
            }
        }
        catch (Exception excecao)
        {
            if (!(excecao instanceof ErroCarregamentoBiblioteca))
            {
                throw new ErroCarregamentoBiblioteca(nome, excecao);
            }
            else
            {
                throw (ErroCarregamentoBiblioteca) excecao;
            }
        }
        
        return null;
    }
    
    public void desregistrarBiblioteca(Biblioteca biblioteca, Programa programa) throws ErroCarregamentoBiblioteca
    {
        try
        {
            MetaDadosBiblioteca metaDadosBiblioteca = obterMetaDadosBiblioteca(biblioteca.getNome());

            if (metaDadosBiblioteca.getTipo() == TipoBiblioteca.RESERVADA)
            {
                if (bibliotecasReservadas.containsKey(programa))
                {
                    Map<String, Biblioteca> memoriaPrograma = bibliotecasReservadas.get(programa);

                    if (memoriaPrograma.containsKey(biblioteca.getNome()))
                    {
                        biblioteca.finalizar();
                        memoriaPrograma.remove(biblioteca.getNome());
                    }
                    
                    bibliotecasReservadas.remove(programa);
                }
            }
        }
        catch (Exception excecao)
        {
            if (!(excecao instanceof ErroCarregamentoBiblioteca))
            {
                throw new ErroCarregamentoBiblioteca(biblioteca.getNome(), excecao);
            }
            else
            {
                throw (ErroCarregamentoBiblioteca) excecao;
            }
        }            
    }
    
    private Class<? extends Biblioteca> carregarBiblioteca(String nome) throws ErroCarregamentoBiblioteca
    {
        if (!bibliotecasCarregadas.containsKey(nome))
        {
            try
            {
                if (!listarBibliotecasDisponiveis().contains(nome))
                {
                    throw new ClassNotFoundException();
                }
                
                Class classeBiblioteca = Class.forName("br.univali.portugol.nucleo.bibliotecas.".concat(nome)).asSubclass(Biblioteca.class);
                bibliotecasCarregadas.put(nome, classeBiblioteca);
                
                return classeBiblioteca;
            }
            catch (ClassNotFoundException | NoClassDefFoundError excecao)
            {
                throw new ErroCarregamentoBiblioteca(nome, "a biblioteca não foi encontrada");
            }
            catch (ClassCastException excecao)
            {
                throw new ErroCarregamentoBiblioteca(nome, "a biblioteca não estende a classe base");
            }
            catch (Exception excecao)
            {
                throw new ErroCarregamentoBiblioteca(nome, excecao);
            }
        }
        
        return bibliotecasCarregadas.get(nome);
    }    
    
    private MetaDadosBiblioteca obterMetaDadosBiblioteca(String nomeBiblioteca, Class<? extends Biblioteca> classeBiblioteca) throws ErroCarregamentoBiblioteca
    {
        if (declaracaoValida(classeBiblioteca))
        {
            PropriedadesBiblioteca propriedadesBiblioteca = obterAnotacaoClasse(nomeBiblioteca, classeBiblioteca, PropriedadesBiblioteca.class);
            DocumentacaoBiblioteca documentacaoBiblioteca = obterAnotacaoClasse(nomeBiblioteca, classeBiblioteca, DocumentacaoBiblioteca.class);

            MetaDadosBiblioteca metaDadosBiblioteca = new MetaDadosBiblioteca();

            metaDadosBiblioteca.setNome(nomeBiblioteca);
            metaDadosBiblioteca.setTipo(propriedadesBiblioteca.tipo());
            metaDadosBiblioteca.setDocumentacao(documentacaoBiblioteca);
            
            MetaDadosConstantes metaDadosConstantes = obterMetaDadosConstantes(nomeBiblioteca, classeBiblioteca);
            MetaDadosFuncoes metaDadosFuncoes = obterMetaDadosFuncoes(nomeBiblioteca, classeBiblioteca);
            
            if (!metaDadosFuncoes.vazio() || !metaDadosConstantes.vazio())
            {
                metaDadosBiblioteca.setMetaDadosFuncoes(metaDadosFuncoes);
                metaDadosBiblioteca.setMetaDadosConstantes(metaDadosConstantes);
            }
            
            else throw new ErroCarregamentoBiblioteca(nomeBiblioteca, "a biblioteca não está exportando nenhuma constante ou função");

            return metaDadosBiblioteca;
        }
        
        else throw new ErroCarregamentoBiblioteca(nomeBiblioteca, montarMensagemDeclaracaoInvalida(classeBiblioteca));
    }
    
    private MetaDadosFuncoes obterMetaDadosFuncoes(String nomeBiblioteca, Class<? extends Biblioteca> classeBiblioteca) throws ErroCarregamentoBiblioteca
    {
        MetaDadosFuncoes metaDadosFuncoes = new MetaDadosFuncoes();
        
        for (Method metodo : classeBiblioteca.getDeclaredMethods())
        {
            if (Modifier.isPublic(metodo.getModifiers()) && metodo.getAnnotation(NaoExportar.class) == null)
            {
                MetaDadosFuncao metaDadosFuncao = obterMetaDadosFuncao(nomeBiblioteca, metodo);
                
                if (!metaDadosFuncoes.contem(metaDadosFuncao.getNome()))
                {
                    metaDadosFuncoes.incluir(metaDadosFuncao);
                }
                
                else throw new ErroCarregamentoBiblioteca(nomeBiblioteca, String.format("o método '%s' possui sobrecargas", metodo.getName()));
            }
        }
        
        return metaDadosFuncoes;
    }
    
    private MetaDadosFuncao obterMetaDadosFuncao(String nomeBiblioteca, Method metodo) throws ErroCarregamentoBiblioteca
    {
        if (!Modifier.isStatic(metodo.getModifiers()))
        {
            if (jogaExcecao(metodo, ErroExecucao.class))
            {
                if (!metodo.getReturnType().isArray())
                {                            
                    DocumentacaoFuncao documentacaoFuncao = obterAnotacaoMetodo(nomeBiblioteca, metodo, DocumentacaoFuncao.class);

                    MetaDadosFuncao metaDadosFuncao = new MetaDadosFuncao();

                    metaDadosFuncao.setNome(metodo.getName());
                    metaDadosFuncao.setDocumentacao(documentacaoFuncao);
                    metaDadosFuncao.setQuantificador(Quantificador.VALOR);
                    metaDadosFuncao.setTipoDado(obterTipoDadoMetodo(nomeBiblioteca, metodo));
                    metaDadosFuncao.setMetaDadosParametros(obterMetaDadosParametros(nomeBiblioteca, metodo, documentacaoFuncao));

                    return metaDadosFuncao;
                }                
                else throw new ErroCarregamentoBiblioteca(nomeBiblioteca, String.format("o retorno do método '%s' não pode ser um vetor nem uma matriz para ser exportado como uma função", metodo.getName()));
            }
            
            else throw new ErroCarregamentoBiblioteca(nomeBiblioteca, String.format("o método '%s' deve jogar uma exceção do tipo '%s' para ser exportado como uma função", metodo.getName(), ErroExecucao.class.getName()));
        }
        
        else throw new ErroCarregamentoBiblioteca(nomeBiblioteca, String.format("o método '%s' não pode ser estático para ser exportado como uma função", metodo.getName()));
    }
    
    private boolean jogaExcecao(Method metodo, Class<? extends Exception> classeExcecao)
    {
        for (int i = 0; i < metodo.getExceptionTypes().length; i++)
        {
            if (metodo.getExceptionTypes()[i] == classeExcecao)
            {
                return true;
            }
        }
        
        return false;
    }
    
    private MetaDadosParametros obterMetaDadosParametros(String nomeBiblioteca, Method metodo, DocumentacaoFuncao documentacaoFuncao) throws ErroCarregamentoBiblioteca
    {
       MetaDadosParametros metaDadosParametros = new MetaDadosParametros();
        
        Class[] tiposParametros = metodo.getParameterTypes();
        Annotation[] anotacoesParametros = documentacaoFuncao.parametros();
        
        for (int indice = 0; indice < tiposParametros.length; indice++)
        {
            if (indice < anotacoesParametros.length)
            {
                DocumentacaoParametro documentacaoParametro = (DocumentacaoParametro) anotacoesParametros[indice];
                MetaDadosParametro metaDadosParametro = new MetaDadosParametro();

                metaDadosParametro.setNome(documentacaoParametro.nome());
                metaDadosParametro.setDocumentacaoParametro(documentacaoParametro);
                metaDadosParametro.setTipoDado(obterTipoDadoParametro(nomeBiblioteca, metodo, indice, documentacaoParametro.nome()));
                metaDadosParametro.setIndice(indice);
                metaDadosParametro.setModoAcesso(obterModoAcessoParametro(metodo, indice));
                metaDadosParametro.setQuantificador(obterQuantificadorParametro(metodo, indice));

                if (!metaDadosParametros.contem(metaDadosParametro.getNome()))
                {
                    metaDadosParametros.incluir(metaDadosParametro);
                }

                else throw new ErroCarregamentoBiblioteca(nomeBiblioteca, String.format("o método '%s' está documentando diferentes parâmetros com o mesmo nome: '%s'", metodo.getName(), documentacaoParametro.nome()));
            }
            
            else throw new ErroCarregamentoBiblioteca(nomeBiblioteca, String.format("o %dº parâmetro da função '%s' não foi documentado com a anotação '%s'", indice + 1, metodo.getName(), DocumentacaoParametro.class.getSimpleName()));
        }
        
        if (anotacoesParametros.length > tiposParametros.length)
        {
            throw new ErroCarregamentoBiblioteca(nomeBiblioteca, String.format("a função '%s' está documentando um parâmetro inexistente: '%s'", metodo.getName(), ((DocumentacaoParametro) anotacoesParametros[anotacoesParametros.length - 1]).nome()));
        }
        
        return metaDadosParametros;
    }
    
    private Quantificador obterQuantificadorParametro(Method metodo, int indice)
    {
        Type tipo = metodo.getGenericParameterTypes()[indice];
        
        if (tipo instanceof ParameterizedType)
        {
            tipo = ((ParameterizedType) tipo).getRawType();
        }
        
        if (tipo == ReferenciaVetor.class)
        {
            return Quantificador.VETOR;
        }
        else if (tipo == ReferenciaMatriz.class)
        {
            return Quantificador.MATRIZ;
        }
        
        return Quantificador.VALOR;
    }
    
    private ModoAcesso obterModoAcessoParametro(Method metodo, int indice)
    {
        Type tipo = metodo.getGenericParameterTypes()[indice];
        
        if (tipo instanceof ParameterizedType)
        {
            tipo = ((ParameterizedType) tipo).getRawType();
        }
        
        if (tipo == ReferenciaVariavel.class || tipo == ReferenciaVetor.class || tipo == ReferenciaMatriz.class)
        {
            return ModoAcesso.POR_REFERENCIA;
        }
        
        return ModoAcesso.POR_VALOR;
    }
    
    private MetaDadosConstantes obterMetaDadosConstantes(String nomeBiblioteca, Class<? extends Biblioteca> classeBiblioteca) throws ErroCarregamentoBiblioteca
    {
        MetaDadosConstantes metaDadosConstantes = new MetaDadosConstantes();
        
        for (Field atributo : classeBiblioteca.getDeclaredFields())
        {
            if (Modifier.isPublic(atributo.getModifiers()))
            {
                if (Modifier.isStatic(atributo.getModifiers()))
                {
                    if (Modifier.isFinal(atributo.getModifiers()))
                    {
                        if (maiusculo(atributo.getName()))
                        {
                            if (!atributo.getType().isArray())
                            {
                                DocumentacaoConstante documentacaoConstante = obterAnotacaoAtributo(nomeBiblioteca, atributo, DocumentacaoConstante.class);
                                MetaDadosConstante metaDadosConstante = new MetaDadosConstante();

                                metaDadosConstante.setNome(atributo.getName());
                                metaDadosConstante.setDocumentacao(documentacaoConstante);
                                metaDadosConstante.setQuantificador(Quantificador.VALOR);
                                metaDadosConstante.setTipoDado(obterTipoDadoConstante(nomeBiblioteca, atributo));

                                try
                                {
                                    metaDadosConstante.setValor(atributo.get(null));
                                }
                                catch (Exception excecao)
                                {
                                    metaDadosConstante.setValor("indefinido");
                                }

                                if (!metaDadosConstantes.contem(metaDadosConstante.getNome()))
                                {
                                    metaDadosConstantes.incluir(metaDadosConstante);
                                }
                            }
                            
                            else throw new ErroCarregamentoBiblioteca(nomeBiblioteca, String.format("o atributo '%s' não pode ser um vetor nem uma matriz para ser exportado como uma constante", atributo.getName()));
                        }
                        
                        else throw new ErroCarregamentoBiblioteca(nomeBiblioteca, String.format("o atributo '%s' deve ter o nome todo em letras maiúsuclas para ser exportado como uma constante", atributo.getName()));
                    }
                    
                    else throw new ErroCarregamentoBiblioteca(nomeBiblioteca, String.format("o atributo '%s' deve ser final para ser exportado como uma constante", atributo.getName()));
                }
                
                else throw new ErroCarregamentoBiblioteca(nomeBiblioteca, String.format("o atributo '%s' deve ser estático para ser exportado como uma constante", atributo.getName()));
            }
        }
        
        return metaDadosConstantes;
    }
    
    private TipoDado obterTipoDadoConstante(String nomeBiblioteca, Field atributo) throws ErroCarregamentoBiblioteca
    {
        Class classeTipo = atributo.getType();
        
        if (classeTipo == Integer.TYPE) throw new ErroCarregamentoBiblioteca(nomeBiblioteca, String.format("o atributo '%s' dever ser do tipo '%s' ao invés do tipo primitivo '%s'", atributo.getName(), Integer.class.getName(), Integer.TYPE.getSimpleName()));
        if (classeTipo == Double.TYPE) throw new ErroCarregamentoBiblioteca(nomeBiblioteca, String.format("o atributo '%s' deve ser do tipo '%s' ao invés do tipo primitivo '%s'", atributo.getName(), Double.class.getName(), Double.TYPE.getSimpleName()));
        if (classeTipo == Character.TYPE) throw new ErroCarregamentoBiblioteca(nomeBiblioteca, String.format("o atributo '%s' deve ser do tipo '%s' ao invés do tipo primitivo '%s'", atributo.getName(), Character.class.getName(), Character.TYPE.getSimpleName()));
        if (classeTipo == Boolean.TYPE) throw new ErroCarregamentoBiblioteca(nomeBiblioteca, String.format("o atributo '%s' deve ser do tipo '%s' ao invés do tipo primitivo '%s'", atributo.getName(), Boolean.class.getName(), Boolean.TYPE.getSimpleName()));
        if ((classeTipo == Void.class || classeTipo == Void.TYPE)) throw new ErroCarregamentoBiblioteca(nomeBiblioteca, String.format("o atributo '%s' não pode ser do tipo primitivo '%s' nem do tipo '%s'", atributo.getName(), Void.TYPE.getName(), classeTipo.getName()));

        TipoDado tipoDado;
        
        if ((tipoDado = TipoDado.obterTipoDadoPeloTipoJava(classeTipo)) != null)
        {        
            return tipoDado;
        }
        
        throw new ErroCarregamentoBiblioteca
        (
            nomeBiblioteca, String.format("o tipo do atributo '%s' deve ser um dos tipos a seguir: '%s', '%s', '%s', '%s', ou '%s'", atributo.getName(),
        
            TipoDado.CADEIA.getTipoJava().getName(),
            TipoDado.CARACTER.getTipoJava().getName(),
            TipoDado.INTEIRO.getTipoJava().getName(),
            TipoDado.LOGICO.getTipoJava().getName(),
            TipoDado.REAL.getTipoJava().getName()
        ));
    }
    
    private boolean maiusculo(String texto)
    {
        return texto.equals(texto.toUpperCase());
    }
    
    private <T extends Annotation> T obterAnotacaoClasse(String nomeBiblioteca, Class<? extends Biblioteca> classeBiblioteca, Class<T> classeAnotacao) throws ErroCarregamentoBiblioteca
    {
        T anotacao;
        
        if ((anotacao = classeBiblioteca.getAnnotation(classeAnotacao)) != null)
        {
            return anotacao;
        }
        
        throw new ErroCarregamentoBiblioteca(nomeBiblioteca, String.format("a biblioteca não foi anotada com a anotação '%s'", classeAnotacao.getSimpleName()));
    }
    
    private <T extends Annotation> T obterAnotacaoAtributo(String nomeBiblioteca, Field atributo, Class<T> classeAnotacao) throws ErroCarregamentoBiblioteca
    {
        T anotacao;
        
        if ((anotacao = atributo.getAnnotation(classeAnotacao)) != null)
        {
            return anotacao;
        }
        
        throw new ErroCarregamentoBiblioteca(nomeBiblioteca, String.format("o atributo '%s' não foi anotado com a anotação '%s'", atributo.getName(), classeAnotacao.getSimpleName()));
    }    
    
    private TipoDado obterTipoDadoMetodo(String nomeBiblioteca, Method metodo) throws ErroCarregamentoBiblioteca
    {
        Class classeTipo = metodo.getReturnType();
        
        if (classeTipo == Integer.TYPE) throw new ErroCarregamentoBiblioteca(nomeBiblioteca, String.format("o retorno do método '%s' deve ser do tipo '%s' ao invés do tipo primitivo '%s'", metodo.getName(), Integer.class.getName(), Integer.TYPE.getSimpleName()));
        if (classeTipo == Double.TYPE) throw new ErroCarregamentoBiblioteca(nomeBiblioteca, String.format("o retorno do método '%s' deve ser do tipo '%s' ao invés do tipo primitivo '%s'", metodo.getName(), Double.class.getName(), Double.TYPE.getSimpleName()));
        if (classeTipo == Character.TYPE) throw new ErroCarregamentoBiblioteca(nomeBiblioteca, String.format("o retorno do método '%s' deve ser do tipo '%s' ao invés do tipo primitivo '%s'", metodo.getName(), Character.class.getName(), Character.TYPE.getSimpleName()));
        if (classeTipo == Boolean.TYPE) throw new ErroCarregamentoBiblioteca(nomeBiblioteca, String.format("o retorno do método '%s' deve ser do tipo '%s' ao invés do tipo primitivo '%s'", metodo.getName(), Boolean.class.getName(), Boolean.TYPE.getSimpleName()));
        if (classeTipo == Void.class) throw new ErroCarregamentoBiblioteca(nomeBiblioteca, String.format("o retorno do método '%s' deve ser do tipo primitivo '%s' ao invés do tipo '%s'", metodo.getName(), Void.TYPE.getName(), classeTipo.getName()));

        TipoDado tipoDado;
        
        if ((tipoDado = TipoDado.obterTipoDadoPeloTipoJava(classeTipo)) != null)
        {        
            if (tipoDado != TipoDado.TODOS)
            {
                return tipoDado;
            }
        }
        
        throw new ErroCarregamentoBiblioteca
        (
            nomeBiblioteca, String.format("o retorno do método '%s' deve ser um dos tipos a seguir: '%s', '%s', '%s', '%s', '%s' ou '%s'", metodo.getName(), 
        
            TipoDado.CADEIA.getTipoJava().getName(),
            TipoDado.CARACTER.getTipoJava().getName(),
            TipoDado.INTEIRO.getTipoJava().getName(),
            TipoDado.LOGICO.getTipoJava().getName(),
            TipoDado.REAL.getTipoJava().getName(),
            TipoDado.VAZIO.getTipoJava().getName()
        ));
    }
    
    private TipoDado obterTipoDadoParametro(String nomeBiblioteca, Method metodo, int indice, String nomeParametro) throws ErroCarregamentoBiblioteca
    {
        Class classeTipo = metodo.getParameterTypes()[indice];
        
        if (eReferencia(classeTipo))
        {
            classeTipo = obterTipoReferencia(metodo.getGenericParameterTypes()[indice]);
            
            if (classeTipo == null)
            {
                throw new ErroCarregamentoBiblioteca(nomeBiblioteca, String.format("o tipo do parâmetro '%s' do método '%s' não foi especificado", nomeParametro, metodo.getName()));
            }
        }
        
        if (classeTipo.isArray())
        {
            throw new ErroCarregamentoBiblioteca(nomeBiblioteca, String.format("o tipo do parâmetro '%s' do método '%s' não pode ser um vetor nem uma matriz", nomeParametro, metodo.getName()));
        }
        
        if (classeTipo == Integer.TYPE) throw new ErroCarregamentoBiblioteca(nomeBiblioteca, String.format("o parâmetro '%s' do método '%s' deve ser do tipo '%s' ao invés do tipo primitivo '%s'", nomeParametro, metodo.getName(), Integer.class.getName(), Integer.TYPE.getSimpleName()));
        if (classeTipo == Double.TYPE) throw new ErroCarregamentoBiblioteca(nomeBiblioteca, String.format("o parâmetro '%s' do método '%s' deve ser do tipo '%s' ao invés do tipo primitivo '%s'", nomeParametro, metodo.getName(), Double.class.getName(), Double.TYPE.getSimpleName()));
        if (classeTipo == Character.TYPE) throw new ErroCarregamentoBiblioteca(nomeBiblioteca, String.format("o parâmetro '%s' do método '%s' deve ser do tipo '%s' ao invés do tipo primitivo '%s'", nomeParametro, metodo.getName(), Character.class.getName(), Character.TYPE.getSimpleName()));
        if (classeTipo == Boolean.TYPE) throw new ErroCarregamentoBiblioteca(nomeBiblioteca, String.format("o parâmetro '%s' do método '%s' deve ser do tipo '%s' ao invés do tipo primitivo '%s'", nomeParametro, metodo.getName(), Boolean.class.getName(), Boolean.TYPE.getSimpleName()));
        if ((classeTipo == Void.class || classeTipo == Void.TYPE)) throw new ErroCarregamentoBiblioteca(nomeBiblioteca, String.format("o parâmetro '%s' do método '%s' não pode ser do tipo primitivo '%s' nem do tipo '%s'", nomeParametro, metodo.getName(), Void.TYPE.getName(), classeTipo.getName()));

        TipoDado tipoDado;
        
        if ((tipoDado = TipoDado.obterTipoDadoPeloTipoJava(classeTipo)) != null)
        {        
            return tipoDado;
        }
        
        throw new ErroCarregamentoBiblioteca
        (
            nomeBiblioteca, String.format("o tipo do parâmetro '%s' do método '%s' deve ser um dos tipos a seguir: '%s', '%s', '%s', '%s', '%s' ou '%s'", nomeParametro, metodo.getName(),
        
            TipoDado.CADEIA.getTipoJava().getName(),
            TipoDado.CARACTER.getTipoJava().getName(),
            TipoDado.INTEIRO.getTipoJava().getName(),
            TipoDado.LOGICO.getTipoJava().getName(),
            TipoDado.REAL.getTipoJava().getName(),
            Object.class.getName()
        ));
    }
    
    private Class obterTipoReferencia(Type tipo)
    {
        if (tipo instanceof ParameterizedType)
        {
            Type[] generics = ((ParameterizedType) tipo).getActualTypeArguments();

            if (generics != null && generics.length > 0)
            {
                return (Class) generics[0];
            }
        }
        
        return null;
    }

    private boolean eReferencia(Class tipo)
    {
        return (tipo == ReferenciaVariavel.class || tipo == ReferenciaVetor.class || tipo == ReferenciaMatriz.class);
    }
    
    private <T extends Annotation> T obterAnotacaoMetodo(String nomeBiblioteca, Method metodo, Class<T> classeAnotacao) throws ErroCarregamentoBiblioteca
    {
        T anotacao;
        
        if ((anotacao = metodo.getAnnotation(classeAnotacao)) != null)
        {
            return anotacao;
        }
        
        throw new ErroCarregamentoBiblioteca(nomeBiblioteca, String.format("a função '%s' não foi anotada com a anotação '%s'", metodo.getName(), classeAnotacao.getSimpleName()));
    }    
    
    private boolean declaracaoValida(Class<? extends Biblioteca> classeBiblioteca) throws ErroCarregamentoBiblioteca
    {
        boolean publica = Modifier.isPublic(classeBiblioteca.getModifiers());
        boolean efinal = Modifier.isFinal(classeBiblioteca.getModifiers());
        boolean estatica = Modifier.isStatic(classeBiblioteca.getModifiers());
        boolean anonima = classeBiblioteca.isAnonymousClass();
        boolean sintetica = classeBiblioteca.isSynthetic();
        boolean membro = classeBiblioteca.isMemberClass();
        boolean local = classeBiblioteca.isLocalClass();
        
        return (publica && efinal && !estatica && !anonima && !sintetica && !membro && !local);
    }
    
    private String montarMensagemDeclaracaoInvalida(Class<? extends Biblioteca> classeBiblioteca)
    {
        if (!Modifier.isPublic(classeBiblioteca.getModifiers()))    return "a biblioteca deve ser pública";
        if (!Modifier.isFinal(classeBiblioteca.getModifiers()))     return "a biblioteca deve ser final";
        if (Modifier.isStatic(classeBiblioteca.getModifiers()))     return "a biblioteca não pode ser estática";
        if (classeBiblioteca.isAnonymousClass())                    return "a biblioteca não pode ser uma classe anônima";
        if (classeBiblioteca.isSynthetic())                         return "a biblioteca não pode ser uma classe sintética";
        if (classeBiblioteca.isMemberClass())                       return "a biblioteca não pode ser uma classe membro";
        if (classeBiblioteca.isLocalClass())                        return "a biblioteca não pode ser uma classe local";
        
        return null;
    }

    @Override
    public void execucaoIniciada(Programa programa)
    {
        /** 
         * Neste ponto, nenhum nó NoInclusaoBiblioteca foi visitado ainda, portanto, 
         * a lista de bibliotecas está vazia. A inicialização é feita no método
         * registrarBiblioteca, chamado pelo Interpretador.
         * 
         **/
    }

    @Override
    public void execucaoEncerrada(Programa programa, ResultadoExecucao resultadoExecucao)
    {
        if (bibliotecasReservadas.containsKey(programa))
        {
            for (Biblioteca biblioteca : bibliotecasReservadas.get(programa).values())
            {
                try
                {
                    biblioteca.finalizar();
                }
                catch (ErroExecucao e)
                {
                    resultadoExecucao.setErro(e);
                }
            }
            
            bibliotecasReservadas.remove(programa);
        }
    }
}