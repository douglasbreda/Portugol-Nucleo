package br.univali.portugol.nucleo.analise.semantica.erros;

import br.univali.portugol.nucleo.asa.NoChamadaFuncao;
import br.univali.portugol.nucleo.asa.NoExpressao;
import br.univali.portugol.nucleo.asa.NoReferenciaMatriz;
import br.univali.portugol.nucleo.asa.NoReferenciaVariavel;
import br.univali.portugol.nucleo.asa.NoReferenciaVetor;
import br.univali.portugol.nucleo.mensagens.ErroSemantico;
import br.univali.portugol.nucleo.simbolos.Funcao;
import br.univali.portugol.nucleo.simbolos.Matriz;
import br.univali.portugol.nucleo.simbolos.Simbolo;
import br.univali.portugol.nucleo.simbolos.Variavel;
import br.univali.portugol.nucleo.simbolos.Vetor;

public class ErroReferenciaInvalida extends ErroSemantico
{
    private final NoExpressao expressao;
    private final Simbolo simbolo;

    public ErroReferenciaInvalida(NoExpressao expressao, Simbolo simbolo)
    {
        super(expressao.getTrechoCodigoFonte());
        this.expressao = expressao;
        this.simbolo = simbolo;
    }

    @Override
    protected String construirMensagem()
    {
        StringBuilder stringBuilder = new StringBuilder();
        String s = null;
        if (simbolo != null){
            if (simbolo instanceof Vetor)
            {
                s = "O vetor '%s' está sendo utilizado como ";
            }
            else if (simbolo instanceof Matriz)
            {
                s = "A matriz '%s' está sendo utilizada como ";
            }
            else if (simbolo instanceof Variavel)
            {
                s = "A variável '%s' está sendo utilizada como ";
            }
            else if (simbolo instanceof Funcao)
            {
                s = "A função '%s' está sendo utilizada como ";
            }
        
            stringBuilder.append(String.format(s, simbolo.getNome()));
        
            if (expressao instanceof NoReferenciaVariavel)
            {
                stringBuilder.append("uma variável");
            }
            else if (expressao instanceof NoReferenciaMatriz)
            {
                stringBuilder.append("uma matriz");
            }
            else if (expressao instanceof NoReferenciaVetor)
            {
                stringBuilder.append("um vetor");
            }
            else if (expressao instanceof NoChamadaFuncao)
            {
                stringBuilder.append("uma função");
            }
        }
        return stringBuilder.toString();
    }    
}