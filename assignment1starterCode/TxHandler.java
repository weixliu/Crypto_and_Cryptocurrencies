import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TxHandler {
    private UTXOPool utxoPool;

    /**
     * Creates a public ledger whose current UTXOPool (collection of unspent transaction outputs) is
     * {@code utxoPool}. This should make a copy of utxoPool by using the UTXOPool(UTXOPool uPool)
     * constructor.
     */
    public TxHandler(UTXOPool utxoPool) {
        // IMPLEMENT THIS
        this.utxoPool = new UTXOPool(utxoPool);
    }

    /**
     * @return true if:
     * (1) all outputs claimed by {@code tx} are in the current UTXO pool,
     * (2) the signatures on each input of {@code tx} are valid,
     * (3) no UTXO is claimed multiple times by {@code tx},
     * (4) all of {@code tx}s output values are non-negative, and
     * (5) the sum of {@code tx}s input values is greater than or equal to the sum of its output
     * values; and false otherwise.
     */
    public boolean isValidTx(Transaction tx) {
        // IMPLEMENT THIS
        //(1)
        for (Transaction.Input txInput : tx.getInputs()) {
            UTXO txInputUTXO = new UTXO(txInput.prevTxHash, txInput.outputIndex);
            if (!utxoPool.contains(txInputUTXO)) {
                return false;
            }
        }
        //(2)
        int intputLen = tx.numInputs();
        for (int i = 0; i < intputLen; i++) {
            int outputIdx = tx.getInput(i).outputIndex;
            UTXO utxo = new UTXO(tx.getInput(i).prevTxHash, outputIdx);
            if (!Crypto.verifySignature(utxoPool.getTxOutput(utxo).address, tx.getRawDataToSign(i), tx.getInput(i).signature)) {
                return false;
            }
        }
        //(3)
        Set<UTXO> utxos = new HashSet<>();
        for (Transaction.Input txInput : tx.getInputs()) {
            UTXO txInputUTXO = new UTXO(txInput.prevTxHash, txInput.outputIndex);
            if (utxos.contains(txInputUTXO)) {
                return false;
            }
            utxos.add(txInputUTXO);
        }
        //(4)
        for (Transaction.Output txOutput : tx.getOutputs()) {
            if (!(txOutput.value >= 0)) {
                return false;
            }
        }
        //(5)
        double inputSum = 0.0;
        for (Transaction.Input txInput : tx.getInputs()) {
            UTXO txInputUTXO = new UTXO(txInput.prevTxHash, txInput.outputIndex);
            inputSum += utxoPool.getTxOutput(txInputUTXO).value;
        }
        double outputSum = 0.0;
        for (Transaction.Output txOutput : tx.getOutputs()) {
            outputSum += txOutput.value;
        }
        if (inputSum < outputSum) {
            return false;
        }
        return true;
    }

    /**
     * Handles each epoch by receiving an unordered array of proposed transactions, checking each
     * transaction for correctness, returning a mutually valid array of accepted transactions, and
     * updating the current UTXO pool as appropriate.
     */
    public Transaction[] handleTxs(Transaction[] possibleTxs) {
        // IMPLEMENT THIS
        Set<Transaction> txSet = new HashSet<>(Arrays.asList(possibleTxs));
        List<Transaction> txs = new ArrayList<>();
        int prevTxsLen = 0;
        do {
            prevTxsLen = txs.size();
            List<Transaction> removedTx = new ArrayList<>();
            for (Transaction tx : txSet) {
                if (isValidTx(tx)) {
                    txs.add(tx);
                    for (int i = 0; i < tx.getInputs().size(); i++) {
                        utxoPool.removeUTXO(new UTXO(tx.getInput(i).prevTxHash, tx.getInput(i).outputIndex));
                    }
                    for (int i = 0; i < tx.getOutputs().size(); i++) {
                        utxoPool.addUTXO(new UTXO(tx.getHash(), i), tx.getOutput(i));
                    }
                    removedTx.add(tx);
                }
            }
            for (Transaction removed : removedTx) {
                txSet.remove(removed);
            }
        } while (txs.size() != prevTxsLen);
        Transaction[] txArray = new Transaction[txs.size()];
        for (int i = 0; i < txArray.length; i++) {
            txArray[i] = txs.get(i);
        }
        return txArray;
    }

}
