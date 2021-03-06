package programas;

import br.univali.portugol.nucleo.mensagens.ErroExecucao;
import br.univali.portugol.nucleo.Programa;
import br.univali.portugol.nucleo.bibliotecas.Util;

public class testeParametroPorReferencia extends Programa
{
    private final Util u_1010 = new Util();
    
    private final int[] REFS_INT = new int[5];
    private final double[] REFS_DOUBLE = new double[1];
    private final boolean[] REFS_BOOLEAN = new boolean[1];
    private final char[] REFS_CHAR = new char[1];
    private final String[] REFS_STRING = new String[2];
    
    private final int INDICE_A_0 = 0;
    private final int INDICE_B_1 = 1;
    private final int INDICE_I_2 = 2;
    private final int INDICE_NUMERO_INTEIRO_3 = 3;
    private final int INDICE_TESTE_4 = 4;
    
    private final int INDICE_R_0 = 0;
    
    private final int INDICE_LOG_0 = 0;    
    
    private final int INDICE_CARAC_0 = 0;

    private final int INDICE_TEXTO_0 = 0;
    private final int INDICE_X_1 = 1;  
    
    public testeParametroPorReferencia() throws ErroExecucao, InterruptedException
    {
        
    }
    
    @Override
    protected void inicializar () throws ErroExecucao, InterruptedException {
        REFS_INT[INDICE_I_2] = -1;
    }

    @Override
    protected void executar(String[] parametros) throws ErroExecucao, InterruptedException
    {  
        REFS_INT[INDICE_A_0] = 2;
        REFS_INT[INDICE_B_1] = 4;
        int m[][] = new int[2][2];
        int v[] = new int[3];
        if (true)
        {
            REFS_STRING[INDICE_TEXTO_0]="";
        }
        
        int c = teste(INDICE_A_0, m, v, INDICE_TEXTO_0) + teste(INDICE_B_1, m, v, INDICE_TEXTO_0) * REFS_INT[INDICE_A_0] * REFS_INT[INDICE_B_1];
        c = teste(INDICE_I_2, m, v, INDICE_TEXTO_0);
        c = u_1010.numero_linhas(m);
        c = u_1010.numero_elementos(v);
        
        REFS_STRING[INDICE_X_1] = "asd";
        REFS_INT[INDICE_NUMERO_INTEIRO_3] = 0;
        REFS_DOUBLE[INDICE_R_0] = 1.0;
        REFS_CHAR[INDICE_CARAC_0] = 'b';
        REFS_BOOLEAN[INDICE_LOG_0] = false;
        teste_tipos(INDICE_X_1, INDICE_NUMERO_INTEIRO_3, INDICE_R_0, INDICE_CARAC_0, INDICE_LOG_0);        
    }
    
    private int teste(int x, int matriz[][], int vetor[], int texto) throws ErroExecucao, InterruptedException
    {
        REFS_INT[x] = REFS_INT[x] * 2;
        matriz[0][0] = matriz[0][0];
        vetor[0] = vetor[0];
        REFS_INT[INDICE_TESTE_4] = -1;
        outro_teste(matriz, vetor, INDICE_TESTE_4);
        return 1;
    }
    
    private void outro_teste(int m[][], int v[], int ref) throws ErroExecucao, InterruptedException
    {
        m[0][0] = v[0];
        v[0] = m[0][0];
    }
    
    private void teste_tipos(int c, int i, int r, int car, int l) throws ErroExecucao, InterruptedException
    {
        REFS_STRING[c] = "asd";
        REFS_INT[i] = 10;
        REFS_DOUBLE[r] = 10.0;
        REFS_CHAR[car] = 'a';
        REFS_BOOLEAN[l] = true;
    }
}
