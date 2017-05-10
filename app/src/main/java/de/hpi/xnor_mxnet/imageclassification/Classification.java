package de.hpi.xnor_mxnet.imageclassification;

public class Classification {
    private String _id;
    private String _label;
    private float _probability;

    Classification(String id, String label, float probability) {
        _id = id; _label = label; _probability = probability;
    }

    public Classification(int id, String label, float probability) {
        _id = String.valueOf(id); _label = label; _probability = probability;
    }

    public String get_id() { return _id; }
    public String get_label() { return _label; }
    public float get_probability() { return _probability; }
}
