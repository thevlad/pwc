import React from 'react';
import { BrowserRouter, Route, Switch } from 'react-router-dom';
// import { Router } from "@reach/router"
import Crawl from './components/crawl/Crawl';
import InitialSearch from './components/initialsearch/InitialSearch';
import SearchResult from './components/searchresult/SearchResult';
import { SearchContextProvider } from './components/searchcontext/SearchContext';
import axios from 'axios';
import { createBrowserHistory } from 'history';
import { MuiThemeProvider, createMuiTheme } from '@material-ui/core/styles';


let theme = createMuiTheme();
// theme = responsiveFontSizes(theme);

const BASE_URL = window.location.origin;
const history = createBrowserHistory();

const App = () => {
  async function getStart() {
    return await axios.get(`${BASE_URL}/start`, {
      headers: {
        'X-Requested-With': 'XMLHttpRequest',
        'withCredentials': 'true'
      }
    }).then(response => {
      // returning the data here allows the caller to get it through another .then(...)
      return response.data
    })
  };

  getStart().then(data => { console.log(data) });
  // console.log('session : ' + session);

  return (
    <MuiThemeProvider theme={theme}>
      <SearchContextProvider>
        <BrowserRouter history={history}>
          <Switch>
            <Route exact path="/" component={Crawl} />
            <Route exact path="/search" component={InitialSearch} />
            <Route exact path="/result" component={SearchResult} />
          </Switch>
        </BrowserRouter>
      </SearchContextProvider>
    </MuiThemeProvider>
  )
}

export {
  App,
  BASE_URL,
  history
}