package de.hpi.xnor_mxnet;

class Classification {
    private String _id;
    private String _label;
    private float _probability;

    Classification(String id, String label, float probability) {
        _id = id; _label = label; _probability = probability;
    }

    public String get_id() { return _id; }
    String get_label() { return _label; }
    float get_probability() { return _probability; }
}
