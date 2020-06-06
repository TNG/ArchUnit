const createJsonFromClassNames = require('./testinfrastructure/class-names-to-json-transformer').createJsonFromClassNames;
const {createDependencies} = require('./testinfrastructure/test-json-creator');
const getGraphUi = require('./testinfrastructure/graph-creator').getGraphUi;

describe('Filtering in Graph', () => {
  it('can filter node by name containing', async () => {
    const jsonRoot = createJsonFromClassNames('com.tngtech.archunit.SomeClass1',
    'com.tngtech.archunit.SomeClass2',
    'com.tngtech.archunit.NotMatchingClass');
    const jsonDependencies = createDependencies()
    .addMethodCall().from('com.tngtech.archunit.SomeClass1', 'startMethod()')
    .to('com.tngtech.archunit.SomeClass2', 'targetMethod()')
    .addMethodCall().from('com.tngtech.archunit.SomeClass2', 'startMethod()')
    .to('com.tngtech.archunit.NotMatchingClass', 'targetMethod()')
    .build();
    const graphUi = await getGraphUi(jsonRoot, jsonDependencies);
    await graphUi.clickNode('com.tngtech.archunit');

    await graphUi.changeNodeFilter('*Some*');

    graphUi.expectOnlyVisibleNodes('SomeClass1', 'SomeClass2', 'com.tngtech.archunit');
    graphUi.expectOnlyVisibleDependencies('com.tngtech.archunit.SomeClass1-com.tngtech.archunit.SomeClass2');
  });

  it('can filter node by control click', async () => {
    const jsonRoot = createJsonFromClassNames('com.tngtech.archunit.CtrlClickClass',
    'com.tngtech.archunit.SomeClass1',
    'com.tngtech.archunit.SomeClass2');
    const jsonDependencies = createDependencies()
    .addMethodCall().from('com.tngtech.archunit.CtrlClickClass', 'startMethod()')
    .to('com.tngtech.archunit.SomeClass1', 'targetMethod()')
    .addMethodCall().from('com.tngtech.archunit.SomeClass1', 'startMethod()')
    .to('com.tngtech.archunit.SomeClass2', 'targetMethod()')
    .build();
    const graphUi = await getGraphUi(jsonRoot, jsonDependencies);
    await graphUi.clickNode('com.tngtech.archunit');

    await graphUi.ctrlClickNode('com.tngtech.archunit.CtrlClickClass');

    graphUi.expectOnlyVisibleNodes('SomeClass1', 'SomeClass2', 'com.tngtech.archunit');
    graphUi.expectOnlyVisibleDependencies('com.tngtech.archunit.SomeClass1-com.tngtech.archunit.SomeClass2');
    graphUi.expectNodeFilter('~com.tngtech.archunit.CtrlClickClass');
  });

  it('can filter multiple nodes by control click', async () => {
    const jsonRoot = createJsonFromClassNames('com.tngtech.archunit.CtrlClickClass',
    'com.tngtech.archunit.SecondCtrlClickClass',
    'com.tngtech.archunit.SomeClass1',
    'com.tngtech.archunit.SomeClass2');
    const jsonDependencies = createDependencies()
    .addMethodCall().from('com.tngtech.archunit.CtrlClickClass', 'startMethod()')
    .to('com.tngtech.archunit.SomeClass1', 'targetMethod()')
    .addMethodCall().from('com.tngtech.archunit.SecondCtrlClickClass', 'startMethod()')
    .to('com.tngtech.archunit.SomeClass1', 'targetMethod()')
    .addMethodCall().from('com.tngtech.archunit.SomeClass1', 'startMethod()')
    .to('com.tngtech.archunit.SomeClass2', 'targetMethod()')
    .build();
    const graphUi = await getGraphUi(jsonRoot, jsonDependencies);
    await graphUi.clickNode('com.tngtech.archunit');

    await graphUi.ctrlClickNode('com.tngtech.archunit.CtrlClickClass');

    graphUi.expectOnlyVisibleNodes('SecondCtrlClickClass', 'SomeClass1', 'SomeClass2', 'com.tngtech.archunit');
    graphUi.expectOnlyVisibleDependencies('com.tngtech.archunit.SecondCtrlClickClass-com.tngtech.archunit.SomeClass1',
    'com.tngtech.archunit.SomeClass1-com.tngtech.archunit.SomeClass2');
    graphUi.expectNodeFilter('~com.tngtech.archunit.CtrlClickClass');

    await graphUi.ctrlClickNode('com.tngtech.archunit.SecondCtrlClickClass');

    graphUi.expectOnlyVisibleNodes('SomeClass1', 'SomeClass2', 'com.tngtech.archunit');
    graphUi.expectOnlyVisibleDependencies('com.tngtech.archunit.SomeClass1-com.tngtech.archunit.SomeClass2');
    graphUi.expectNodeFilter('~com.tngtech.archunit.CtrlClickClass|~com.tngtech.archunit.SecondCtrlClickClass');
  });

  it('can filter node by name not containing', async () => {
    const jsonRoot = createJsonFromClassNames('com.tngtech.archunit.SomeClass1',
    'com.tngtech.archunit.SomeClass2',
    'com.tngtech.archunit.MatchingClass');
    const jsonDependencies = createDependencies()
    .addMethodCall().from('com.tngtech.archunit.SomeClass1', 'startMethod()')
    .to('com.tngtech.archunit.SomeClass2', 'targetMethod()')
    .addMethodCall().from('com.tngtech.archunit.SomeClass2', 'startMethod()')
    .to('com.tngtech.archunit.MatchingClass', 'targetMethod()')
    .build();
    const graphUi = await getGraphUi(jsonRoot, jsonDependencies);
    await graphUi.clickNode('com.tngtech.archunit');

    await graphUi.changeNodeFilter('~*Matching*');

    graphUi.expectOnlyVisibleNodes('com.tngtech.archunit', 'SomeClass1', 'SomeClass2');
    graphUi.expectOnlyVisibleDependencies('com.tngtech.archunit.SomeClass1-com.tngtech.archunit.SomeClass2');
  });

  it('can filter nodes by type', async () => {
    const jsonRoot = createJsonFromClassNames('com.tngtech.archunit.SomeClass1',
    'com.tngtech.archunit.SomeClass2',
    'com.tngtech.archunit.SomeInterface');
    const jsonDependencies = createDependencies()
    .addMethodCall().from('com.tngtech.archunit.SomeClass1', 'startMethod()')
    .to('com.tngtech.archunit.SomeClass2', 'targetMethod()')
    .addMethodCall().from('com.tngtech.archunit.SomeClass2', 'startMethod()')
    .to('com.tngtech.archunit.SomeInterface', 'targetMethod()')
    .build();
    const graphUi = await getGraphUi(jsonRoot, jsonDependencies);
    await graphUi.clickNode('com.tngtech.archunit');

    await graphUi.filterNodesByType({showInterfaces: false, showClasses: true});

    graphUi.expectOnlyVisibleNodes('com.tngtech.archunit', 'SomeClass1', 'SomeClass2');
    graphUi.expectOnlyVisibleDependencies('com.tngtech.archunit.SomeClass1-com.tngtech.archunit.SomeClass2');
  });

  const dataProvider = [{
    message: 'can filter dependencies by type (no dependency types)',
    filteredDependencies: {INHERITANCE: false, CONSTRUCTOR_CALL: false, METHOD_CALL: false, FIELD_ACCESS: false},
    expectedDependencies: [],
  }, {
    message: 'can filter dependencies by type (inheritance)',
    filteredDependencies: {INHERITANCE: true, CONSTRUCTOR_CALL: false, METHOD_CALL: false, FIELD_ACCESS: false},
    expectedDependencies: ['com.tngtech.archunit.InheritanceClass-com.tngtech.archunit.TargetClass'],
  }, {
    message: 'can filter dependencies by type (constructor call)',
    filteredDependencies: {INHERITANCE: false, CONSTRUCTOR_CALL: true, METHOD_CALL: false, FIELD_ACCESS: false},
    expectedDependencies: ['com.tngtech.archunit.ConstructorClass-com.tngtech.archunit.TargetClass'],
  }, {
    message: 'can filter dependencies by type (method call)',
    filteredDependencies: {INHERITANCE: false, CONSTRUCTOR_CALL: false, METHOD_CALL: true, FIELD_ACCESS: false},
    expectedDependencies: ['com.tngtech.archunit.MethodClass-com.tngtech.archunit.TargetClass'],
  }, {
    message: 'can filter dependencies by type (field access)',
    filteredDependencies: {INHERITANCE: false, CONSTRUCTOR_CALL: false, METHOD_CALL: false, FIELD_ACCESS: true},
    expectedDependencies: ['com.tngtech.archunit.FieldAccessClass-com.tngtech.archunit.TargetClass'],
  }, {
    message: 'can filter dependencies by type (all dependency types)',
    filteredDependencies: {INHERITANCE: true, CONSTRUCTOR_CALL: true, METHOD_CALL: true, FIELD_ACCESS: true},
    expectedDependencies: [
      'com.tngtech.archunit.InheritanceClass-com.tngtech.archunit.TargetClass',
      'com.tngtech.archunit.ConstructorClass-com.tngtech.archunit.TargetClass',
      'com.tngtech.archunit.MethodClass-com.tngtech.archunit.TargetClass',
      'com.tngtech.archunit.FieldAccessClass-com.tngtech.archunit.TargetClass'
    ],
  }];

  dataProvider.forEach(({message, filteredDependencies, expectedDependencies}) => it(message, async () => {
    const jsonRoot = createJsonFromClassNames('com.tngtech.archunit.MethodClass',
    'com.tngtech.archunit.InheritanceClass',
    'com.tngtech.archunit.ConstructorClass',
    'com.tngtech.archunit.FieldAccessClass',
    'com.tngtech.archunit.TargetClass');
    const jsonDependencies = createDependencies()
    .addMethodCall().from('com.tngtech.archunit.MethodClass', 'startMethod()')
    .to('com.tngtech.archunit.TargetClass', 'targetMethod()')
    .addConstructorCall().from('com.tngtech.archunit.ConstructorClass', 'startMethod()')
    .to('com.tngtech.archunit.TargetClass', 'init()')
    .addFieldAccess().from('com.tngtech.archunit.FieldAccessClass', 'startMethod()')
    .to('com.tngtech.archunit.TargetClass', 'targetMethod()')
    .addInheritance().from('com.tngtech.archunit.InheritanceClass')
    .to('com.tngtech.archunit.TargetClass')
    .build();
    const graphUi = await getGraphUi(jsonRoot, jsonDependencies);
    await graphUi.clickNode('com.tngtech.archunit');

    graphUi.expectOnlyVisibleDependencies(
    'com.tngtech.archunit.MethodClass-com.tngtech.archunit.TargetClass',
    'com.tngtech.archunit.ConstructorClass-com.tngtech.archunit.TargetClass',
    'com.tngtech.archunit.FieldAccessClass-com.tngtech.archunit.TargetClass',
    'com.tngtech.archunit.InheritanceClass-com.tngtech.archunit.TargetClass'
    );

    await graphUi.filterDependenciesByType(filteredDependencies);

    graphUi.expectOnlyVisibleDependencies(expectedDependencies);
  }));

  it('can filter nodes and dependencies by type and violation and node name', async () => {
    const jsonRoot = createJsonFromClassNames('com.tngtech.archunit.MethodClass',
    'com.tngtech.archunit.InheritanceClass',
    'com.tngtech.archunit.SomeInterface',
    'com.tngtech.archunit.SomeClass',
    'com.tngtech.archunit.TargetClass');
    const jsonDependencies = createDependencies()
    .addMethodCall().from('com.tngtech.archunit.MethodClass', 'startMethod()')
    .to('com.tngtech.archunit.TargetClass', 'targetMethod()')
    .addInheritance().from('com.tngtech.archunit.SomeInterface', 'startMethod()')
    .to('com.tngtech.archunit.InheritanceClass')
    .addInheritance().from('com.tngtech.archunit.InheritanceClass')
    .to('com.tngtech.archunit.TargetClass')
    .build();
    const violations = [{
      rule: 'rule1',
      violations: ['<com.tngtech.archunit.SomeClass> INHERITANCE to <com.tngtech.archunit.TargetClass>']
    }];
    const graphUi = await getGraphUi(jsonRoot, jsonDependencies, violations);
    await graphUi.clickNode('com.tngtech.archunit');

    graphUi.expectOnlyVisibleNodes('MethodClass', 'InheritanceClass', 'SomeInterface', 'SomeClass', 'TargetClass', 'com.tngtech.archunit')
    graphUi.expectOnlyVisibleDependencies('com.tngtech.archunit.SomeInterface-com.tngtech.archunit.InheritanceClass',
    'com.tngtech.archunit.MethodClass-com.tngtech.archunit.TargetClass',
    'com.tngtech.archunit.InheritanceClass-com.tngtech.archunit.TargetClass');

    await graphUi.filterNodesByType({showInterfaces: false, showClasses: true});

    graphUi.expectOnlyVisibleNodes('MethodClass', 'InheritanceClass', 'SomeClass', 'TargetClass', 'com.tngtech.archunit')
    graphUi.expectOnlyVisibleDependencies('com.tngtech.archunit.MethodClass-com.tngtech.archunit.TargetClass',
    'com.tngtech.archunit.InheritanceClass-com.tngtech.archunit.TargetClass');

    await graphUi.filterDependenciesByType({INHERITANCE: true, CONSTRUCTOR_CALL: false, METHOD_CALL: false, FIELD_ACCESS: false});

    graphUi.expectOnlyVisibleNodes('MethodClass', 'InheritanceClass', 'SomeClass', 'TargetClass', 'com.tngtech.archunit')
    graphUi.expectOnlyVisibleDependencies('com.tngtech.archunit.InheritanceClass-com.tngtech.archunit.TargetClass');

    await graphUi.ctrlClickNode('com.tngtech.archunit.TargetClass');

    graphUi.expectOnlyVisibleNodes('MethodClass', 'InheritanceClass', 'SomeClass', 'com.tngtech.archunit')
    graphUi.expectOnlyVisibleDependencies([]);
  });
});
