import React, { useState } from 'react';

const SearchContext = React.createContext([{}, () => { }]);

const SearchContextProvider = (props) => {

    const initState = {
        searchQuery: { searchStr: "Search Here", page: 1, total: 0, pages: 0, onlySubmittedDomain: false, domain: null },
        searchResult: [],
    }
    const [state, setState] = useState(initState);

    return (
        <SearchContext.Provider value={[state, setState]}>
            {props.children}
        </SearchContext.Provider>
    );
}

export {
    SearchContext,
    SearchContextProvider
}