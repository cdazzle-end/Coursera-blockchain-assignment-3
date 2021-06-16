package blockchain;

import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Arrays;

public class TxHandler {

    /**
     * Creates a public ledger whose current UTXOPool (collection of unspent transaction outputs) is
     * {@code utxoPool}. This should make a copy of utxoPool by using the UTXOPool(UTXOPool uPool)
     * constructor.
     */
    
    public UTXOPool currentPool;
    
    public TxHandler(UTXOPool utxoPool) {
        // IMPLEMEN T THIS
        currentPool = utxoPool;
    }

    public UTXOPool getUTXOPool(){
        return currentPool;
    }
    /**
     * @return true if:
     * (1) all outputs claimed by {@code tx} are in the current UTXO pool, 
     * (2) the signatures on each input of {@code tx} are valid, 
     * (3) no UTXO is claimed multiple times by {@code tx},
     * (4) all of {@code tx}s output values are non-negative, and
     * (5) the sum of {@code tx}s input values is greater than or equal to the sum of its output
     *     values; and false otherwise.
     */
    public boolean isValidTx(Transaction tx) {
        // IMPLEMENT THIS
        ArrayList<Transaction.Output> outputs = tx.getOutputs();

        //get corresponding utxos from the tx inputs
        ArrayList<Transaction.Input> inputs = tx.getInputs();
        ArrayList<UTXO> txUTXO = new ArrayList<>();
        double totalTxInput = 0; 
        double totalTxOutput = 0;
        
        //4 Check if all output values are non-negative
        for(Transaction.Output o:outputs){
            if(o.value < 0){
                return false;
            }
            totalTxOutput += o.value;
        }
        
        for(Transaction.Input i:inputs){
            UTXO currentInputUtxo = new UTXO(i.prevTxHash, i.outputIndex);
            
            //3 Check if input utxo was already used
            if(txUTXO.contains(currentInputUtxo)){
                return false;
            }
            
            //1 Check if input utxo is in the list of current utxos
            if(currentPool.contains(currentInputUtxo)){
                txUTXO.add(currentInputUtxo);
            } else {
                return false;
            }
            
            //calculate total value of input utxos
            totalTxInput += currentPool.getTxOutput(currentInputUtxo).value;
            
            //2 Check if the signature on inputs are valid
            PublicKey utxoPubKey = currentPool.getTxOutput(currentInputUtxo).address;
            byte[] inputSig = i.signature;
            int inputIndex = inputs.indexOf(i);
            byte[] sigMessage = tx.getRawDataToSign(inputIndex);
            if(!Crypto.verifySignature(utxoPubKey, sigMessage, inputSig)){
                return false;
            }
        }
        
        //5 Check if input value is greater than output
        if(totalTxInput < totalTxOutput){
            return false;
        }
        return true;
    }
    

    /**
     * Handles each epoch by receiving an unordered array of proposed transactions, checking each
     * transaction for correctness, returning a mutually valid array of accepted transactions, and
     * updating the current UTXO pool as appropriate.
     * @param possibleTxs
     * @return 
     */
    public Transaction[] handleTxs(Transaction[] possibleTxs) {
        // IMPLEMENT THIS
        if(possibleTxs == null)
            return new Transaction[0];
        
        ArrayList<Transaction> validTxs = new ArrayList<>();
        
        //iterate through each transaction, validate, remove spent UTXO, add new UTXO
        for(int i = 0; i < possibleTxs.length; i++){
            
            //validate tx
            if(isValidTx(possibleTxs[i])){
                possibleTxs[i].finalize();
                validTxs.add(possibleTxs[i]);
                
                //remove valid/spent UTXOs from current pool
                for(Transaction.Input x:possibleTxs[i].getInputs()){
                    UTXO inputUTXO = new UTXO(x.prevTxHash, x.outputIndex);
                    currentPool.removeUTXO(inputUTXO);
                }
                
                //add new UTXOs to current pool
                int utxoIndex = 0;
                for(Transaction.Output x:possibleTxs[i].getOutputs()){
                    UTXO outputUTXO = new UTXO(possibleTxs[i].getHash(), utxoIndex);
                    currentPool.addUTXO(outputUTXO, x);
                    utxoIndex++;
                }
                
            }
        }
        
        //return list of validated 
        Transaction[] validatedTxs = new Transaction[validTxs.size()];
        
        validatedTxs = validTxs.toArray(validatedTxs);
        
        return validatedTxs;
    }

}



//import java.util.ArrayList;
//import java.util.concurrent.ConcurrentHashMap;
//import java.util.HashSet;
//import java.security.PublicKey;
//import java.util.Set;
//
//public class TxHandler {
//    
//    /**
//     * Creates a public ledger whose current UTXOPool (collection of unspent transaction outputs) is
//     * {@code utxoPool}. This should make a copy of utxoPool by using the UTXOPool(UTXOPool uPool)
//     * constructor.
//     */
//    
//    private UTXOPool pool;
//    private TxValidator validator = new TxValidator();
//
//    public TxHandler(UTXOPool utxoPool) {
//        this.pool = new UTXOPool(utxoPool);
//    }
//
//    private class TxValidator {
//        private double sumIn = 0, sumOut = 0;
//
//        public boolean validate(UTXOPool pool, Transaction tx) {
//            return notNull(tx) && inputValid(pool, tx) && outputValid(tx);
//        }
//
//        private boolean notNull(Transaction tx) {
//            return tx != null;
//        }
//
//        private boolean inputValid(UTXOPool pool, Transaction tx) {
//            Set<UTXO> usedTxs = new HashSet<>();
//            sumIn = 0;
//            
//          // enforcing rule 1
//            for (int c = 0; c < tx.numInputs(); c++) {
//                Transaction.Input input = tx.getInput(c);
//                if (input == null) {
//                    return false;
//                }
//                
//                UTXO utxo = new UTXO(input.prevTxHash, input.outputIndex);
//
//                if (!pool.contains(utxo) || usedTxs.contains(utxo)) {
//                    return false;
//                }
//                Transaction.Output prevTxOut = pool.getTxOutput(utxo);
//
//                // enforcing rule 2
//                PublicKey pubKey = prevTxOut.address;
//                byte[] message = tx.getRawDataToSign(c);
//                byte[] signature = input.signature;
//                if (!Crypto.verifySignature(pubKey, message, signature)) {
//                    return false;
//                }
//
//                // enforcing rule 3
//                usedTxs.add(utxo);
//                sumIn += prevTxOut.value;
//            }
//
//            return true;
//        }
//
//        private boolean outputValid(Transaction tx) {
//            // enforcing rule 4
//            sumOut = 0;
//            for (int c = 0; c < tx.numOutputs(); c++) {
//                Transaction.Output out = tx.getOutput(c);
//                if (out.value < 0) {
//                    return false;
//                }
//                sumOut += out.value;
//            }
//
//            // enforcing rule 5
//            return sumIn >= sumOut;
//        }
//    }
//
//    /**
//     * @return true if:
//     * (1) all outputs claimed by {@code tx} are in the current UTXO pool,
//     * (2) the signatures on each input of {@code tx} are valid,
//     * (3) no UTXO is claimed multiple times by {@code tx},
//     * (4) all of {@code tx}s output values are non-negative, and
//     * (5) the sum of {@code tx}s input values is greater than or equal to the sum of its output
//     * values; and false otherwise.
//     */
//    public boolean isValidTx(Transaction tx) {
//        return validator.validate(this.pool, tx);
//    }
//
//    private ConcurrentHashMap<byte[], Transaction> getTxMap(Transaction[] possibleTxs) {
//        ConcurrentHashMap<byte[], Transaction> txs = new ConcurrentHashMap<>();
//
//        for (Transaction tx : possibleTxs) {
//            if (tx == null) {
//                continue;
//            }
//            
//            tx.finalize();
//            txs.put(tx.getHash(), tx);
//        }
//        return txs;
//    }
//
//    /**
//     * Handles each epoch by receiving an unordered array of proposed transactions, checking each
//     * transaction for correctness, returning a mutually valid array of accepted transactions, and
//     * updating the current UTXO pool as appropriate.
//     */
//    
//    public Transaction[] handleTxs(Transaction[] possibleTxs) {
//        if (possibleTxs == null) {
//            return new Transaction[0];
//        }
//        ConcurrentHashMap<byte[], Transaction> txs = getTxMap(possibleTxs);
//
//        ArrayList<Transaction> valid = new ArrayList<>();
//        int txCount;
//        do {
//            txCount = txs.size();
//            for (Transaction tx : txs.values()) {
//                if (!isValidTx(tx)) {
//                    continue;
//                }
//                valid.add(tx);
//                this.applyTx(tx);
//                txs.remove(tx.getHash());
//            }
//            if (txCount == txs.size() || txCount == 0) { 
//                break; 
//            }
//            
//        } while (true);
//
//        return valid.toArray(new Transaction[valid.size()]);
//    }
//
//    private void applyTx(Transaction tx) {
//        if (tx == null) {
//            return;
//        }
//        for (Transaction.Input input : tx.getInputs()) {
//            UTXO utxo = new UTXO(input.prevTxHash, input.outputIndex);
//            this.pool.removeUTXO(utxo);
//        }
//        byte[] txHash = tx.getHash();
//        int transactionLocator = 0;
//        for (Transaction.Output output : tx.getOutputs()) {
//            UTXO utxo = new UTXO(txHash, transactionLocator);
//            transactionLocator += 1;
//            this.pool.addUTXO(utxo, output);
//        }
//    }
//}