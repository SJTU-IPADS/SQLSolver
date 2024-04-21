package sqlsolver.superopt.substitution;

import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import sqlsolver.common.utils.ListSupport;
import sqlsolver.superopt.util.Fingerprint;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Predicate;

class SubstitutionBankImpl implements SubstitutionBank {
  private final Map<String, Substitution> rules;
  private final Multimap<String, String> fingerprintIndex;
  private boolean isExtended;

  SubstitutionBankImpl() {
    this.rules = new LinkedHashMap<>(2048);
    this.fingerprintIndex = MultimapBuilder.hashKeys(2048).arrayListValues(32).build();
    this.isExtended = false;
  }

  @Override
  public boolean isExtended() {
    return isExtended;
  }

  @Override
  public int size() {
    assert rules.size() == fingerprintIndex.size();
    return rules.size();
  }

  @Override
  public Collection<Substitution> rules() {
    return rules.values();
  }

  @Override
  public boolean add(Substitution rule) {
    final String identity = rule.canonicalStringify();
    if (rules.containsKey(identity)) return false;
    rule.setId(rules.size() + 1);
    rules.put(identity, rule);
    fingerprintIndex.put(Fingerprint.mk(rule._0()).toString(), identity);
    if (!isExtended) isExtended = identity.contains("Union") || identity.contains("Agg");
    return true;
  }

  @Override
  public void remove(Substitution o) {
    final String identity = o.canonicalStringify();
    final Substitution removed = rules.remove(identity);
    if (removed != null) fingerprintIndex.remove(Fingerprint.mk(o._0()).toString(), identity);
  }

  @Override
  public void removeIf(Predicate<Substitution> check) {
    final var iter = rules.entrySet().iterator();
    while (iter.hasNext()) {
      final Substitution rule = iter.next().getValue();
      final String identity = rule.canonicalStringify();
      if (check.test(rule)) {
        iter.remove();
        fingerprintIndex.remove(Fingerprint.mk(rule._0()).toString(), identity);
      }
    }
  }

  @Override
  public boolean contains(Substitution rule) {
    return rules.containsKey(rule.canonicalStringify());
  }

  @Override
  public Iterable<Substitution> ruleOfFingerprint(Fingerprint fingerprint) {
    return ListSupport.map(fingerprintIndex.get(fingerprint.fingerprint()), rules::get);
  }
}
